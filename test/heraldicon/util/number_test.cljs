(ns heraldicon.util.number-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [heraldicon.util.number :as number]))

(deftest to-roman
  (are [n roman] (= (number/to-roman n) roman)
    1 "I"
    2 "II"
    4 "IV"
    5 "V"
    6 "VI"
    9 "IX"
    10 "X"
    20 "XX"
    40 "XL"
    50 "L"
    90 "XC"
    100 "C"
    400 "CD"
    500 "D"
    900 "CM"
    1000 "M"
    1949 "MCMXLIX"
    2013 "MMXIII"
    3999 "MMMCMXCIX"))

(deftest from-roman
  (are [n roman] (= (number/from-roman roman) n)
    1 "I"
    2 "II"
    4 "IV"
    ;; should also be accepted
    4 "IIII"
    5 "V"
    6 "VI"
    9 "IX"
    10 "X"
    20 "XX"
    40 "XL"
    50 "L"
    90 "XC"
    100 "C"
    400 "CD"
    500 "D"
    900 "CM"
    1000 "M"
    1949 "MCMXLIX"
    2013 "MMXIII"
    3999 "MMMCMXCIX"))
