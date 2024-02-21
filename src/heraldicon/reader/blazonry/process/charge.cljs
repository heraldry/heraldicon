(ns heraldicon.reader.blazonry.process.charge
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.reader.blazonry.result :as result]))

(defn- find-best-variant [{:keys [type attitude facing]} charge-map]
  (let [short-charge-type (-> type name keyword)
        candidates (get charge-map short-charge-type)
        candidates-with-attitude (cond->> candidates
                                   attitude (filter (fn [charge]
                                                      (-> charge
                                                          :data
                                                          :attitude
                                                          (or :rampant)
                                                          (= attitude)))))
        charge-name (-> short-charge-type
                        name
                        (str/replace "-" " "))
        [attitude-warning
         candidates] (if (and attitude
                              (seq candidates)
                              (empty? candidates-with-attitude))
                       [(str "No charge '"
                             charge-name
                             "' found with attitude '"
                             (-> attitude
                                 name
                                 (str/replace "-" " "))
                             "', using best match.")
                        candidates]
                       [nil
                        candidates-with-attitude])
        candidates-with-facing (cond->> candidates
                                 facing (filter (fn [charge]
                                                  (-> charge
                                                      :data
                                                      :facing
                                                      (or :to-dexter)
                                                      (= facing)))))
        [facing-warning
         candidates] (if (and facing
                              (seq candidates)
                              (empty? candidates-with-facing))
                       [(str "No charge '"
                             charge-name
                             "' found facing '"
                             (-> facing
                                 name
                                 (str/replace "-" " "))
                             "', using best match.")
                        candidates]
                       [nil
                        candidates-with-facing])
        warnings (cond-> []
                   attitude-warning (conj attitude-warning)
                   facing-warning (conj facing-warning))]
    (-> candidates
        first
        ;; TODO: this is not ideal
        (or {:id nil
             :version nil})
        (select-keys [:id :version])
        (cond->
          (seq warnings) (assoc ::result/warnings warnings)))))

(defn- is-charge-type? [charge-type]
  (some-> charge-type namespace (= "heraldry.charge.type")))

(defn- populate-charge-variants [{:keys [charge-map]} hdn]
  (if (map? hdn)
    (let [charge-type (:type hdn)]
      (if (and (is-charge-type? charge-type)
               (not (get charge.options/charge-map charge-type)))
        (let [variant (find-best-variant hdn charge-map)]
          (cond-> hdn
            variant (assoc :variant variant)))
        hdn))
    hdn))

(defn process [hdn parser]
  (walk/postwalk (partial populate-charge-variants parser) hdn))
