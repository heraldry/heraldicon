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
   {:context [:root]}))

(def square-french
  (field-environment/create
   (str "m 0,0"
        "v 15.7"
        "c 0,6 6,12 12,13"
        "c 6,-1 12,-7 12,-13"
        "V 0"
        "z")
   {:context [:root]}))

(def square-iberian
  (field-environment/create
   (str "m 0,0"
        "h 5"
        "v 7"
        "a 5 5 0 0 1 -10,0"
        "v -7"
        "z")
   {:context [:root]}))

(def kinds
  [["Heater" :heater heater]
   ["Square French" :square-french square-french]
   ["Square Iberian" :square-iberian square-iberian]])

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
