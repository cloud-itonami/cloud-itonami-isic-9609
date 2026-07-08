(ns personalservice.personalserviceadvisor
  "PersonalServiceOps-LLM client -- the *contained intelligence node*
  for the other-personal-service (matchmaking/introduction/personal-
  concierge) actor.

  It normalizes client intake, drafts a per-jurisdiction personal-
  referral-service evidence checklist, screens clients for an
  uncleared background check, and drafts the referral-finalization
  action. CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record or a real referral finalization. Every output is
  censored downstream by `personalservice.governor` before anything
  touches the SSoT, and `:actuation/finalize-referral` proposals NEVER
  auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/finalize-referral | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [personalservice.facts :as facts]
            [personalservice.registry :as registry]
            [personalservice.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the client, contract or jurisdiction. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "顧客記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :client/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-serviceplan
  "Per-jurisdiction personal-referral-service evidence checklist draft.
  `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a jurisdiction with NO official spec-basis
  in `personalservice.facts` -- the Personal Service Governor must
  reject this (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [c (store/client db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction c))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "personalservice.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :serviceplan/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :serviceplan/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-background-check
  "Client-facing background-check screening draft -- the SAME literal
  concept `school.schooladvisor`'s advisor established first and
  `sports.sportsadvisor/screen-background-check` already reused
  literally as the second instance (see `personalservice.governor`'s
  ns docstring for the full ordinal accounting). `:background-check-
  not-cleared?` on the client record injects the failure mode the
  Personal Service Governor must HOLD, un-overridably, on."
  [db {:keys [subject]}]
  (let [c (store/client db subject)]
    (cond
      (nil? c)
      {:summary "対象顧客記録が見つかりません" :rationale "no client record"
       :cites [] :effect :background-check/set :value {:client-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:background-check-not-cleared? c))
      {:summary    (str (:client-name c) ": 身元確認が未完了")
       :rationale  "スクリーニングが身元確認未完了を検出。人手確認とホールドが必須。"
       :cites      [:background-check]
       :effect     :background-check/set
       :value      {:client-id subject :verdict :not-cleared}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:client-name c) ": 身元確認は完了")
       :rationale  "身元確認スクリーニング完了。"
       :cites      [:background-check]
       :effect     :background-check/set
       :value      {:client-id subject :verdict :cleared}
       :stake      nil
       :confidence 0.9})))

(defn- propose-referral-finalization
  "Draft the actual REFERRAL-FINALIZATION action -- finalizing a real
  personal referral or introduction. ALWAYS `:stake :actuation/
  finalize-referral` -- this is a REAL-WORLD act, never a draft the
  actor may auto-run. See README `Actuation`: no phase ever adds this
  op to a phase's `:auto` set (`personalservice.phase`); the governor
  also always escalates on `:actuation/finalize-referral`. Two
  independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [c (store/client db subject)
        safe? (and c (not (registry/cooling-off-period-not-elapsed? c))
                   (not (:background-check-not-cleared? c)))]
    {:summary    (str subject " 向け紹介確定提案"
                      (when c (str " (client=" (:client-name c) ")")))
     :rationale  (if c
                   (str "days-since-contract-signed=" (:days-since-contract-signed c)
                        " minimum-cooling-off-period-days=" registry/minimum-cooling-off-period-days)
                   "顧客記録が見つかりません")
     :cites      (if c [subject] [])
     :effect     :client/mark-finalized
     :value      {:client-id subject}
     :stake      :actuation/finalize-referral
     :confidence (if safe? 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :client/intake              (normalize-intake db request)
    :serviceplan/verify          (verify-serviceplan db request)
    :background-check/screen    (screen-background-check db request)
    :actuation/finalize-referral (propose-referral-finalization db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたはパーソナルサービス事業(結婚相手紹介等)の紹介確定エージェントの"
       "助言者です。与えられた事実のみに基づき、提案を1つだけEDNマップで"
       "返します。説明や前置きは一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:client/upsert|:serviceplan/set|:background-check/set|"
       ":client/mark-finalized) "
       ":stake(:actuation/finalize-referral か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :serviceplan/verify          {:client (store/client st subject)}
    :background-check/screen     {:client (store/client st subject)}
    :actuation/finalize-referral  {:client (store/client st subject)}
    {:client (store/client st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Personal Service Governor
  escalates/holds -- an LLM hiccup can never auto-finalize a
  referral."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :personalserviceadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
