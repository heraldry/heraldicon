(ns heraldry.test-svg
  (:require [cljs.test :refer-macros [deftest are]]
            [heraldry.vector.svg :as svg]
            [heraldry.vector.core :as v]
            [taoensso.timbre :as log]))

(deftest reverse-path
  (are [path start reversed-path] (let [result (svg/reverse-path path)
                                        expected {:start start
                                                  :path (svg/normalize-path-relative reversed-path)}]
                                    (when (not= result expected)
                                      (log/debug "\nexp:" expected "\ngot:" result))
                                    (= result expected))
    "M0,0 l5,0 h45 v10" (v/v 50 10) "M0,0 v-10 h-45 l-5,0"))
