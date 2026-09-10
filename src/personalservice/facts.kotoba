(ns personalservice.facts
  "Per-jurisdiction personal-service/dating-and-introduction-service
  regulatory catalog -- the G2-style spec-basis table the Personal
  Service Governor checks every `:serviceplan/verify` proposal
  against ('did the advisor cite an OFFICIAL public source for this
  jurisdiction's personal-referral-service/consumer-cancellation
  framework, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official consumer-
  protection/dating-service-contract authority (see `:provenance`);
  they are a STARTING catalog, not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the client-
  consent/service-plan/background-check-verification/referral-
  completion evidence set this blueprint's own Offer names;
  `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any `:actuation/finalize-
  referral` proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "消費者庁 (Consumer Affairs Agency)"
          :legal-basis "特定商取引に関する法律 (Act on Specified Commercial Transactions) -- 特定継続的役務提供 (結婚相手紹介サービス等)"
          :national-spec "結婚相手紹介サービス等の特定継続的役務提供に係るクーリング・オフ期間および中途解約規定"
          :provenance "https://www.caa.go.jp/policies/policy/consumer_transaction/specified_commercial_transactions/"
          :required-evidence ["顧客同意記録 (client-consent-record)"
                              "サービス計画記録 (service-plan-record)"
                              "身元確認記録 (background-check-verification-record)"
                              "紹介完了記録 (referral-completion-record)"]}
   "USA" {:name "United States"
          :owner-authority "California Department of Consumer Affairs"
          :legal-basis "California Civil Code §1694 et seq. (Dating Service Contracts Act)"
          :national-spec "Mandatory 3-business-day cancellation period for dating-service contracts"
          :provenance "https://leginfo.legislature.ca.gov/faces/codes_displaySection.xhtml?lawCode=CIV&sectionNum=1694"
          :required-evidence ["Client consent record"
                              "Service-plan record"
                              "Background-check-verification record"
                              "Referral-completion record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Competition and Markets Authority (CMA)"
          :legal-basis "Consumer Contracts (Information, Cancellation and Additional Charges) Regulations 2013"
          :national-spec "14-day cancellation period for off-premises/distance personal-service contracts"
          :provenance "https://www.legislation.gov.uk/uksi/2013/3134/contents"
          :required-evidence ["Client consent record"
                              "Service-plan record"
                              "Background-check-verification record"
                              "Referral-completion record"]}
   "DEU" {:name "Germany"
          :owner-authority "Bundesministerium der Justiz (BMJ)"
          :legal-basis "Bürgerliches Gesetzbuch (BGB) §355 (Widerrufsrecht)"
          :national-spec "14-tägiges Widerrufsrecht für Verbraucherverträge über persönliche Dienstleistungen"
          :provenance "https://www.gesetze-im-internet.de/bgb/__355.html"
          :required-evidence ["Einwilligungsprotokoll (client-consent-record)"
                              "Serviceplanprotokoll (service-plan-record)"
                              "Zuverlässigkeitsnachweis (background-check-verification-record)"
                              "Vermittlungsabschlussprotokoll (referral-completion-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to finalize a
  referral on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-9609 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `personalservice.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
