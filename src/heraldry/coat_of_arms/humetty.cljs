(ns heraldry.coat-of-arms.humetty
  (:require
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

(defn options [context]
  (let [humetty? (interface/get-raw-data (c/++ context :humetty?))]
    (cond-> {:humetty? {:type :boolean
                        :ui {:label :string.option/humetty}}
             :ui {:label :string.option/humetty
                  :tooltip :string.tooltip/humetty-warning
                  :form-type :humetty}}
      humetty? (assoc :corner {:type :choice
                               :choices [[:string.option.corner-choice/round :round]
                                         [:string.option.corner-choice/sharp :sharp]
                                         [:string.option.corner-choice/bevel :bevel]]
                               :default :round
                               :ui {:label :string.option/corner}}
                      :distance {:type :range
                                 :min 1
                                 :max 45
                                 :default 5
                                 :ui {:label :string.option/distance}}))))

(defn coup [shape base-distance {:keys [environment] :as context}]
  (let [shape (if (map? shape)
                shape
                {:paths [shape]})]
    (if (interface/get-sanitized-data (c/++ context :humetty?))
      (let [distance (interface/get-sanitized-data (c/++ context :distance))
            distance ((util/percent-of base-distance) distance)
            corner (interface/get-sanitized-data (c/++ context :corner))
            environment-shape (environment/effective-shape environment)
            shrunken-environment-shape (environment/shrink-shape environment-shape distance corner)
            ;; TODO: this currently expects a single path
            couped-shape (environment/intersect-shapes
                          (-> shape :paths first)
                          shrunken-environment-shape)]
        (assoc shape :paths [couped-shape]))
      shape)))
