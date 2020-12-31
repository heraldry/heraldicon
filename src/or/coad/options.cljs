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

(defn get-sanitized-value-or-nil [value options]
  (if (nil? value)
    nil
    (case (:type options)
      :choice (let [choices (into #{}
                                  (map second (:choices options)))]
                (if (contains? choices value)
                  value
                  nil))
      :range  (if (nil? value)
                nil
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

(defn sanitize [values given-options]
  (into {}
        (for [[k v] given-options]
          (cond
            (and (map? v)
                 (not (contains?
                       types (:type v)))) [k (sanitize (get values k) v)]
            :else                         [k (get-value (get values k) v)]))))

(defn sanitize-or-nil [values given-options]
  (into {}
        (for [[k v] given-options]
          (cond
            (and (map? v)
                 (not (contains?
                       types (:type v)))) [k (sanitize-or-nil (get values k) v)]
            :else                         [k (get-sanitized-value-or-nil (get values k) v)]))))
