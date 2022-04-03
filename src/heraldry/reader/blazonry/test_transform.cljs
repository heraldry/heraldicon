(ns heraldry.reader.blazonry.test-transform
  (:require
   [cljs.test :refer-macros [are deftest]]
   [heraldry.reader.blazonry.transform :as transform]))

(deftest transforming
  (are [ast form] (= (transform/ast->hdn ast) form)

    [:A "a"] 1
    [:A "an"] 1

    [:NUMBER "123"] 123
    [:NUMBER "0134"] 134

    [:number-word [:DIGIT-WORD "one"]] 1
    [:number-word "twelve"] 12
    [:number-word [:MULTILPE-OF-TEN-WORD "eighty"]] 80
    [:number-word [:MULTILPE-OF-TEN-WORD "eighty"] " " [:DIGIT-WORD "one"]] 81
    [:number-word [:MULTILPE-OF-TEN-WORD "eighty"] "-" [:DIGIT-WORD "one"]] 81
    [:number-word [:MULTILPE-OF-TEN-WORD "eighty"] "" [:DIGIT-WORD "one"]] 81
    [:number-word [:MULTILPE-OF-TEN-WORD "twenty"] "" [:DIGIT-WORD "seven"]] 27

    [:number-word [:MULTI-WORD "double"]] 2
    [:number-word [:MULTI-WORD "triple"]] 3))
