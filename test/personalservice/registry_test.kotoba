(ns personalservice.registry-test
  (:require [clojure.test :refer [deftest is]]
            [personalservice.registry :as r]))

;; ----------------------------- cooling-off-period-not-elapsed? -----------------------------

(deftest not-elapsed-when-below-minimum
  (is (r/cooling-off-period-not-elapsed? {:days-since-contract-signed 0}))
  (is (r/cooling-off-period-not-elapsed? {:days-since-contract-signed 1}))
  (is (r/cooling-off-period-not-elapsed? {:days-since-contract-signed 2})))

(deftest elapsed-at-and-past-minimum
  (is (not (r/cooling-off-period-not-elapsed? {:days-since-contract-signed 3})))
  (is (not (r/cooling-off-period-not-elapsed? {:days-since-contract-signed 4})))
  (is (not (r/cooling-off-period-not-elapsed? {:days-since-contract-signed 30}))))

(deftest missing-field-is-not-treated-as-elapsed
  (is (not (r/cooling-off-period-not-elapsed? {})))
  (is (not (r/cooling-off-period-not-elapsed? {:days-since-contract-signed nil}))))

;; ----------------------------- register-referral-finalization -----------------------------

(deftest finalization-is-a-draft-not-a-real-finalization
  (let [result (r/register-referral-finalization "client-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest finalization-assigns-referral-number
  (let [result (r/register-referral-finalization "client-1" "JPN" 7)]
    (is (= (get result "referral_number") "JPN-REF-000007"))
    (is (= (get-in result ["record" "client_id"]) "client-1"))
    (is (= (get-in result ["record" "kind"]) "referral-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest finalization-validation-rules
  (is (thrown? Exception (r/register-referral-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-referral-finalization "client-1" "" 0)))
  (is (thrown? Exception (r/register-referral-finalization "client-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-referral-finalization "client-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-referral-finalization "client-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-REF-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-REF-000001" (get-in hist2 [1 "record_id"])))))
