(ns or.coad.options)

(def types #{:range :choice})

(defn get-value [value options]
  (let [value (or value (:default options))]
    (case (:type options)
      :choice (let [choices (into #{}
                                  (map second (:choices options)))]
                (if (contains? choices value)
                  value
                  (-> options :choices first second)))
      :range  (if (nil? value)
                (:min options)
                (-> value
                    (max (:min options))
                    (min (:max options))))
      value)))

#_{:clj-kondo/ignore [:redefined-var]}
(defn merge
  [x other]
  (let [x (if (nil? x) {} x)]
    (into x
          (for [[key value] other]
            (let [other-value (get x key)]
              (cond
                (nil? value)             [key nil]
                (and (contains? x key)
                     (nil? other-value)) [key nil]
                (map? value)             [key (merge other-value value)]
                :else                    [key (cond
                                                (= key :min) (max value other-value)
                                                (= key :max) (min value other-value)
                                                :else        value)]))))))

#_(merge {:origin        {:point    {:type    :choice
                                     :choices [["Fess" :fess]
                                               ["Chief" :chief]]
                                     :default :fess}
                          :offset-x {:type    :range
                                     :min     -45
                                     :max     45
                                     :default 0}
                          :offset-y {:type    :range
                                     :min     -45
                                     :max     45
                                     :default 0}}
          :diagonal-mode {:type    :choice
                          :choices [["foo" :foo]]}
          :line          {:style        {:choices [["Straight" :straight]
                                                   ["Invected" :invected]]
                                         :default :straight}
                          :eccentricity {:type :range
                                         :min  10
                                         :max  20}}}
         {:origin        {:offset-x nil}
          :diagonal-mode nil
          :line          {:eccentricity {:max 4}}
          :new-option    {:type :range
                          :min  5}})
