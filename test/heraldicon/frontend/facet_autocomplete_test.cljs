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

(deftest apply-suggestion-replaces-last-token
  (is (= "tincture:" (ac/apply-suggestion "tin" "tincture:")))
  (is (= "smith tincture:or"
         (ac/apply-suggestion "smith tincture:o" "tincture:or")))
  (testing "no whitespace → suggestion replaces entire input"
    (is (= "tincture:or" (ac/apply-suggestion "" "tincture:or")))))
