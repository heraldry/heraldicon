(ns heraldicon.util.number
  (:require
   [clojure.string :as s]))

;; https://gist.github.com/jimweirich/1388782
(def ^:private roman-reductions
  '((1000 "M")
    (900 "CM") (500 "D") (400 "CD") (100 "C")
    (90 "XC") (50 "L") (40 "XL") (10 "X")
    (9 "IX") (5 "V") (4 "IV") (1 "I")))

(defn to-roman [number]
  (let [counts
        (map first
             (drop 1
                   (reductions (fn [[_ r] v]
                                 (list (int (/ r v))
                                       (- r (* v (int (/ r v))))))
                               (list 0 number)
                               (map first roman-reductions))))
        glyphs (map second roman-reductions)]
    (s/join
     (flatten
      (map (fn [[c g]] (take c (repeat g)))
           (map vector counts glyphs))))))

(defn- add-numeral [n t]
  (if (> n (* 4 t))
    (- n t)
    (+ t n)))

(def ^:private numerals
  {\I 1
   \V 5
   \X 10
   \L 50
   \C 100
   \D 500
   \M 1000})

(defn from-roman [s]
  (reduce add-numeral (map numerals (reverse (s/upper-case s)))))
