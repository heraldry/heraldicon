(ns or.coad.escutcheon
  (:require [or.coad.field :as field]))

(def heater
  ;; sqrt(3) / 2 * 6 ~ 5.196152422706632
  (field/make-field (str "m 0,0"
                         "h 3"
                         "v 2"
                         "a 6 6 0 0 1 -3,5.196152422706632"
                         "a 6 6 0 0 1 -3,-5.196152422706632"
                         "v -2"
                         "z")
                    {:context [:root]}))

(def square-iberian
  (field/make-field (str "m 0,0"
                         "h 5"
                         "v 7"
                         "a 5 5 0 0 1 -10,0"
                         "v -7"
                         "z")
                    {:context [:root]}))

(def kinds
  [["Heater" :heater heater]
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
