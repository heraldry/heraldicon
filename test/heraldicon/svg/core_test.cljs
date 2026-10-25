(ns heraldicon.svg.core-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [heraldicon.svg.core :as svg]))

(deftest parse-css-rules
  (testing "rules with selector lists and declarations"
    (is (= (#'svg/parse-css-rules ".st0{fill:#FFFFFF;stroke-miterlimit:10;}\n.a, #b ,path { stroke : none }")
           [[[".st0"] {:fill "#FFFFFF" :stroke-miterlimit "10"}]
            [[".a" "#b" "path"] {:stroke "none"}]])))

  (testing "comments and at-rules are skipped"
    (is (= (#'svg/parse-css-rules (str "/* exported */ @import url(\"x.css\");\n"
                                       "@font-face { font-family: 'X'; src: url(data:font/woff;base64,AA) }\n"
                                       ".a{fill:red}\n"
                                       "@media screen { .b { fill: blue } }\n"
                                       ".c{stroke:green}"))
           [[[".a"] {:fill "red"}]
            [[".c"] {:stroke "green"}]])))

  (testing "semicolons inside parentheses or quotes don't split declarations"
    (is (= (#'svg/parse-css-rules ".d{ fill: url('data:image/png;base64,AA=='); stroke:none }")
           [[[".d"] {:fill "url('data:image/png;base64,AA==')" :stroke "none"}]])))

  (testing "broken input doesn't throw"
    (is (= (#'svg/parse-css-rules ".e{fill:red")
           [[[".e"] {:fill "red"}]]))))

(deftest sanitize-charge-data
  (testing "drawing elements and their attributes stay"
    (let [data [:svg {:viewBox "0 0 10 10"}
                [:defs {} [:linearGradient {:id "a"} [:stop {:offset "0"}]]]
                [:g {:fill "url(#a)" :transform "translate(1 1)"}
                 [:path {:d "M0 0h1"}]
                 [:use {:href "#b"}]
                 [:image {:href "data:image/png;base64,AAAA"}]]]]
      (is (= (svg/sanitize-charge-data data) data))))

  (testing "namespaced and lower-cased tags count as their element"
    (is (= (svg/sanitize-charge-data [:svg:g {} [:lineargradient {}]])
           [:svg:g {} [:lineargradient {}]])))

  (testing "elements outside the allowlist are removed"
    (is (= (svg/sanitize-charge-data [:svg {}
                                      [:foreignObject {} [:div {} "text"]]
                                      [:style {} ".a{}"]
                                      [:path {:d "M0 0"}]])
           [:svg {} nil nil [:path {:d "M0 0"}]])))

  (testing "event attributes are removed"
    (is (= (svg/sanitize-charge-data [:rect {:width "1" :onload "noop" :onClick "noop"}])
           [:rect {:width "1"}])))

  (testing "links other than fragments and embedded images are removed"
    (is (= (svg/sanitize-charge-data [:g {}
                                      [:use {:href "https://example.com/x.svg#b"}]
                                      [:image {:xlink:href "https://example.com/x.png"}]])
           [:g {}
            [:use {}]
            [:image {}]])))

  (testing "links become groups, keeping their content"
    (is (= (svg/sanitize-charge-data [:a {:href "https://example.com" :transform "scale(2)"}
                                      [:path {:d "M0 0"}]])
           [:g {:transform "scale(2)"} [:path {:d "M0 0"}]]))))
