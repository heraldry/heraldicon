(ns heraldicon.reader.blazonry.test-transform
  (:require
   [cljs.test :refer-macros [are deftest]]
   [heraldicon.reader.blazonry.transform :as transform]))

(deftest transforming
  (are [ast form] (= (transform/ast->hdn ast) form)

    [:A "a"] 1
    [:A "an"] 1

    [:number/NUMBER "123"] 123
    [:number/NUMBER "0134"] 134

    [:number-word [:number/DIGIT-WORD "one"]] 1
    [:number-word "twelve"] 12
    [:number-word [:number/MULTILPE-OF-TEN-WORD "eighty"]] 80
    [:number-word [:number/MULTILPE-OF-TEN-WORD "eighty"] " " [:number/DIGIT-WORD "one"]] 81
    [:number-word [:number/MULTILPE-OF-TEN-WORD "eighty"] "-" [:number/DIGIT-WORD "one"]] 81
    [:number-word [:number/MULTILPE-OF-TEN-WORD "eighty"] "" [:number/DIGIT-WORD "one"]] 81
    [:number-word [:number/MULTILPE-OF-TEN-WORD "twenty"] "" [:number/DIGIT-WORD "seven"]] 27

    [:number-word [:number/MULTI-WORD "double"]] 2
    [:number-word [:number/MULTI-WORD "triple"]] 3))
