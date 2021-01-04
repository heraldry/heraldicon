(ns heraldry.coat-of-arms.position
  (:require [heraldry.coat-of-arms.vector :as v]))

(def choices
  [["Fess" :fess]
   ["Chief" :chief]
   ["Base" :base]
   ["Dexter" :dexter]
   ["Sinister" :sinister]
   ["Honour" :honour]
   ["Nombril" :nombril]])

(def default-options
  {:point {:type :choice
           :choices choices
           :default :fess}
   :offset-x {:type :range
              :min -45
              :max 45
              :default 0}
   :offset-y {:type :range
              :min -45
              :max 45
              :default 0}})

(defn options [type]
  [(get {} type)
   default-options])

(defn calculate [{:keys [point offset-x offset-y] :or {offset-x 0
                                                       offset-y 0}} environment default]
  (let [ref (-> point
                (or default))
        p (-> environment :points (get ref))
        width (:width environment)
        height (:height environment)
        dx (-> offset-x
               (* width)
               (/ 100))
        dy (-> offset-y
               (* height)
               (/ 100)
               -)]
    (v/v (-> p
             :x
             (+ dx))
         (-> p
             :y
             (+ dy)))))
