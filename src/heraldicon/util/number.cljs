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

(def ^:private digits
  #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9})

(def ^:private number-strings
  {"one" 1
   "two" 2
   "double" 2
   "three" 3
   "triple" 3
   "four" 4
   "five" 5
   "six" 6
   "seven" 7
   "eight" 8
   "nine" 9
   "ten" 10
   "eleven" 11
   "twelve" 12
   "thirteen" 13
   "fourteen" 14
   "fifteen" 15
   "sixteen" 16
   "seventeen" 17
   "eighteen" 18
   "nineteen" 19
   "twenty" 20
   "thirty" 30
   "forty" 40
   "fifty" 50
   "sixty" 60
   "seventy" 70
   "eighty" 80
   "ninety" 90})

(defn from-string [s]
  (cond
    (s/blank? s) nil
    (every? digits s) (js/parseInt s)
    (every? numerals (some-> s s/upper-case)) (from-roman s)
    :else (get number-strings (some-> s s/lower-case))))

(defn- ordinal-plain [s]
  (when-let [s (some-> s s/trim)]
    (cond
      (or (s/ends-with? s ".")
          (s/ends-with? s ":")) (from-string (subs s 0 (dec (count s))))
      (or (s/ends-with? s "st")
          (s/ends-with? s "nd")
          (s/ends-with? s "rd")
          (s/ends-with? s "th")) (from-string (subs s 0 (- (count s) 2)))
      :else nil)))

(def ^:private ordinals
  {"first" 1
   "second" 2
   "third" 3
   "fourth" 4
   "fifth" 5
   "sixth" 6
   "seventh" 7
   "eighth" 8
   "ninth" 9
   "tenth" 10
   "eleventh" 11
   "twelfth" 12
   "thirteenth" 13
   "fourteenth" 14
   "fifteenth" 15
   "sixteenth" 16
   "seventeenth" 17
   "eighteenth" 18
   "nineteenth" 19
   "twentieth" 20})

(defn ordinal-from-string [s]
  (or (ordinal-plain s)
      (ordinals s)))
