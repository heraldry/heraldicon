(ns heraldicon.svg.core-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [heraldicon.svg.core :as svg]))

(deftest sanitize-charge-data
  (testing "drawing elements and their attributes stay"
    (let [data [:svg {:viewBox "0 0 10 10"}
                [:defs {} [:linearGradient {:id "a"} [:stop {:offset "0"}]]]
                [:g {:fill "url(#a)" :transform "translate(1 1)"}
                 [:path {:d "M0 0h1"}]
                 [:use {:href "#b"}]
                 [:image {:href "data:image/png;base64,AAAA"}]]]]
      (is (= data (svg/sanitize-charge-data data)))))

  (testing "namespaced and lower-cased tags count as their element"
    (is (= [:svg:g {} [:lineargradient {}]]
           (svg/sanitize-charge-data [:svg:g {} [:lineargradient {}]]))))

  (testing "elements outside the allowlist are removed"
    (is (= [:svg {} nil nil [:path {:d "M0 0"}]]
           (svg/sanitize-charge-data [:svg {}
                                      [:foreignObject {} [:div {} "text"]]
                                      [:style {} ".a{}"]
                                      [:path {:d "M0 0"}]]))))

  (testing "event attributes are removed"
    (is (= [:rect {:width "1"}]
           (svg/sanitize-charge-data [:rect {:width "1" :onload "noop" :onClick "noop"}]))))

  (testing "links other than fragments and embedded images are removed"
    (is (= [:g {}
            [:use {}]
            [:image {}]]
           (svg/sanitize-charge-data [:g {}
                                      [:use {:href "https://example.com/x.svg#b"}]
                                      [:image {:xlink:href "https://example.com/x.png"}]]))))

  (testing "links become groups, keeping their content"
    (is (= [:g {:transform "scale(2)"} [:path {:d "M0 0"}]]
           (svg/sanitize-charge-data [:a {:href "https://example.com" :transform "scale(2)"}
                                      [:path {:d "M0 0"}]])))))
