(ns or.coad.escutcheon
  (:require [or.coad.field-environment :as field-environment]))

(def heater
  ;; sqrt(3) / 2 * 6 ~ 5.196152422706632
  (field-environment/create
   (str "m 0,0"
        "h 3"
        "v 2"
        "a 6 6 0 0 1 -3,5.196152422706632"
        "a 6 6 0 0 1 -3,-5.196152422706632"
        "v -2"
        "z")
   {:context      [:root]
    :bounding-box [-3 3 0 (+ 2 5.196152422706632)]}))

(def square-french
  (field-environment/create
   (str "m 0,0"
        "v 15.7"
        "c 0,6 6,12 12,13"
        "c 6,-1 12,-7 12,-13"
        "V 0"
        "z")
   {:context      [:root]
    :bounding-box [0 (* 2 12) 0 (+ 15.7 13)]}))

(def square-iberian
  (field-environment/create
   (str "m 0,0"
        "h 5"
        "v 7"
        "a 5 5 0 0 1 -10,0"
        "v -7"
        "z")
   {:context      [:root]
    :bounding-box [-5 5 0 (+ 7 5)]}))

(def french-modern
  (field-environment/create
   (str "m 0,0"
        "h 7"
        "v 15"
        "a 1 1 0 0 1 -1,1"
        "h -5"
        "a 1 1 0 0 0 -1,1"
        "a 1 1 0 0 0 -1,-1"
        "h -5"
        "a 1 1 0 0 1 -1,-1"
        "v -15"
        "h 7"
        "z")
   {:context      [:root]
    :bounding-box [-7 7 0 (* 2 8)]}))

(def kinds
  [["Heater" :heater heater]
   ["Square French" :square-french square-french]
   ["Square Iberian" :square-iberian square-iberian]
   ["French Modern" :french-modern french-modern]])

(def kinds-map
  (->> kinds
       (map (fn [[_ key data]]
              [key data]))
       (into {})))

(def options
  (->> kinds
       (map (fn [[name key _]]
              [key name]))))

(defn field [type]
  (get kinds-map type))
