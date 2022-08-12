(ns heraldicon.heraldry.ordinary.shared
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.ordinary.humetty :as humetty]
   [heraldicon.heraldry.ordinary.voided :as voided]
   [heraldicon.interface :as interface]))

(defn add-humetty-and-voided [options context]
  (let [humetty? (interface/get-raw-data (c/++ context :humetty :humetty?))]
    (-> options
        (assoc :voided (voided/options (c/++ context :voided))
               :humetty (humetty/options (c/++ context :humetty)))
        (cond->
          humetty? (->
                     (update :opposite-line dissoc :fimbriation)
                     (update :extra-line dissoc :fimbriation))))))
