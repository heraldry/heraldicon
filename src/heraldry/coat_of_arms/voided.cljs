(ns heraldry.coat-of-arms.voided
  (:require
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

(defn options [context]
  (let [voided? (interface/get-raw-data (c/++ context :voided?))]
    (cond-> {:voided? {:type :boolean
                       :ui {:label (string "Voided")}}
             :ui {:label (string "Voided")
                  :tooltip (string "This might have some strange results for some values, try to give lines a little offset or adjust the parameters a bit if that happens.")
                  :form-type :voided}}
      voided? (assoc :corner {:type :choice
                              :choices [[(string "Round") :round]
                                        [(string "Sharp") :sharp]
                                        [(string "Bevel") :bevel]]
                              :default :sharp
                              :ui {:label (string "Corners")}}
                     :thickness {:type :range
                                 :min 1
                                 :max 45
                                 :default 10
                                 :ui {:label (string "Thickness")}}))))

(defn void [shape base-thickness {:keys [environment] :as context}]
  (let [shape (if (map? shape)
                shape
                {:paths [shape]})]
    (if (interface/get-sanitized-data (c/++ context :voided?))
      (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
            thickness ((util/percent-of base-thickness) thickness)
            corner (interface/get-sanitized-data (c/++ context :corner))
            environment-shape (environment/effective-shape
                               environment
                               :additional-shape shape)
            inner-shape (environment/shrink-shape environment-shape thickness corner)]
        (update shape :paths conj inner-shape))
      shape)))
