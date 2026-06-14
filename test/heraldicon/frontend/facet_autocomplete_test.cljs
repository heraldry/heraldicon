(ns heraldicon.frontend.facet-autocomplete-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [heraldicon.frontend.facet-autocomplete :as ac]))

(deftest no-suggestions-for-empty-or-trailing-space
  (is (nil? (ac/suggestions "")))
  (is (nil? (ac/suggestions nil)))
  (is (nil? (ac/suggestions "smith "))))

(deftest key-prefix-suggests-keys
  (testing "partial key matches a key suggestion ending with ':'"
    (is (some #{"tincture:"} (ac/suggestions "tin"))))
  (testing "case-insensitive match"
    (is (some #{"tincture:"} (ac/suggestions "TIN"))))
  (testing "free-text token that doesn't prefix any key yields no suggestions"
    (is (empty? (ac/suggestions "smith")))))

(deftest known-key-with-colon-suggests-values
  (testing "tincture: shows tincture values"
    (let [s (ac/suggestions "tincture:")]
      (is (some #{"tincture:or"} s))
      (is (some #{"tincture:azure"} s))))
  (testing "partial value filters"
    (let [s (ac/suggestions "tincture:o")]
      (is (some #{"tincture:or"} s))
      (is (not-any? #{"tincture:azure"} s))))
  (testing "preserves prior tokens in the input — suggestions are for the last token only"
    (is (some #{"tincture:or"} (ac/suggestions "smith tincture:o")))))

(deftest charge-and-crest-have-no-static-value-list
  (testing "no values offered for charge:, crest: — user types freely"
    (is (nil? (ac/suggestions "charge:")))
    (is (nil? (ac/suggestions "crest:")))))

(deftest apply-suggestion-replaces-token-at-cursor
  (testing "cursor parks at the end of the inserted value"
    (is (= ["tincture:" (count "tincture:")]
           (ac/apply-suggestion "tin" "tincture:")))
    (is (= ["tincture:or" (count "tincture:or")]
           (ac/apply-suggestion "" "tincture:or"))))
  (testing "tail is left untouched, cursor at end of just-inserted value"
    (is (= ["tincture:or charge:lion" (count "tincture:or")]
           (ac/apply-suggestion "tincture:o charge:lion" 10 "tincture:or")))
    (is (= ["tincture:argent charge:lion" (count "tincture:argent")]
           (ac/apply-suggestion "tincture:or charge:lion" 5 "tincture:argent")))))

(deftest tree-key-recognizes-charge-and-crest
  (is (= "charge" (ac/tree-key "charge:")))
  (is (= "charge" (ac/tree-key "smith charge:lion")))
  (is (= "crest" (ac/tree-key "crest:l")))
  (testing "non-tree keys are nil"
    (is (nil? (ac/tree-key "tincture:or")))
    (is (nil? (ac/tree-key "ornament:helm"))))
  (testing "free text or partial key is nil"
    (is (nil? (ac/tree-key "")))
    (is (nil? (ac/tree-key "charge")))
    (is (nil? (ac/tree-key "smith")))))

(deftest cursor-aware-current-token
  (testing "cursor inside the first of multiple tokens picks that token"
    ;; "tincture:or charge:lion"
    ;; positions: 0123456789012345678901234
    ;; cursor 5 is inside "tincture:or"
    (is (some #{"tincture:or"} (ac/suggestions "tincture:or charge:lion" 5)))
    (is (nil? (ac/tree-key "tincture:or charge:lion" 5))))
  (testing "cursor inside the second token picks that token"
    ;; cursor 18 is inside "charge:lion"
    (is (= "charge" (ac/tree-key "tincture:or charge:lion" 18))))
  (testing "cursor at end of input picks the last token (default behavior)"
    (is (= "charge" (ac/tree-key "tincture:or charge:lion")))))

(deftest current-value-returns-value-portion
  (is (= "lion" (ac/current-value "charge:lion")))
  (is (= "" (ac/current-value "charge:")))
  (testing "follows the cursor across multiple tokens"
    ;; "charge:lion crest:eagle" — cursor in middle of second token
    (is (= "eagle" (ac/current-value "charge:lion crest:eagle" 20)))
    ;; cursor in first token
    (is (= "lion" (ac/current-value "charge:lion crest:eagle" 8)))))
