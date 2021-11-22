(ns heraldry.coat-of-arms.voided
  (:require
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(defn options [context]
  (let [voided? (interface/get-raw-data (c/++ context :voided?))]
    (cond-> {:voided? {:type :boolean
                       :ui {:label strings/voided}}
             :ui {:label strings/voided
                  :tooltip strings/unstable-warning
                  :form-type :voided}}
      voided? (assoc :corner {:type :choice
                              :choices [[strings/corner-round :round]
                                        [strings/corner-sharp :sharp]
                                        [strings/corner-bevel :bevel]]
                              :default :sharp
                              :ui {:label strings/corner}}
                     :thickness {:type :range
                                 :min 1
                                 :max 45
                                 :default 10
                                 :ui {:label strings/thickness}}))))

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
