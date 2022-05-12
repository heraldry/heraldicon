(ns heraldicon.heraldry.ordinary.voided
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]))

(defn options [context]
  (let [voided? (interface/get-raw-data (c/++ context :voided?))]
    (cond-> {:voided? {:type :boolean
                       :ui {:label :string.charge.attribute/voided}}
             :ui {:label :string.charge.attribute/voided
                  :tooltip :string.tooltip/humetty-warning
                  :form-type :voided}}
      voided? (assoc :corner {:type :choice
                              :choices [[:string.option.corner-choice/round :round]
                                        [:string.option.corner-choice/sharp :sharp]
                                        [:string.option.corner-choice/bevel :bevel]]
                              :default :sharp
                              :ui {:label :string.option/corner}}
                     :thickness {:type :range
                                 :min 1
                                 :max 45
                                 :default 10
                                 :ui {:label :string.option/thickness}}))))

(defn void [shape base-thickness {:keys [environment] :as context}]
  (let [shape (if (map? shape)
                shape
                {:paths [shape]})]
    (if (interface/get-sanitized-data (c/++ context :voided?))
      (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
            thickness ((math/percent-of base-thickness) thickness)
            corner (interface/get-sanitized-data (c/++ context :corner))
            environment-shape (environment/effective-shape
                               environment
                               :additional-shape shape)
            inner-shape (environment/shrink-shape environment-shape thickness corner)]
        (update shape :paths conj inner-shape))
      shape)))
