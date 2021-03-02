(ns heraldry.coat-of-arms.test-svg
  (:require [cljs.test :refer-macros [deftest are]]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(deftest reverse-path
  (are [path start reversed-path] (let [result (svg/reverse-path path)
                                        expected {:start start
                                                  :path (svg/normalize-path-relative reversed-path)}]
                                    (when (not= result expected)
                                      (println "got:" result)
                                      (println "exp:" expected))
                                    (= result expected))
    "M0,0 l5,0 h45 v10" (v/v 50 10) "M0,0 v-10 h-45 l-5,0"))
