(ns heraldry.coat-of-arms.charge.core
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(defmethod interface/render-component :heraldry.component/charge [path parent-path environment context]
  [charge-interface/render-charge path parent-path environment context])

(defn title [path context]
  (let [charge-type (interface/get-raw-data (conj path :type) context)
        attitude (or (interface/get-raw-data (conj path :attitude) context)
                     :none)
        facing (or (interface/get-raw-data (conj path :facing) context)
                   :none)]
    (util/combine " " [(util/translate-cap-first charge-type)
                       (when-not (= attitude :none)
                         (util/translate attitude))
                       (when-not (#{:none :to-dexter} facing)
                         (util/translate facing))])))

(defmethod interface/blazon-component :heraldry.component/charge [path {:keys [load-charge-data] :as context}]
  (let [charge-type (interface/get-raw-data (conj path :type) context)
        attitude (interface/get-sanitized-data (conj path :attitude) context)
        facing (interface/get-sanitized-data (conj path :facing) context)
        variant (interface/get-raw-data (conj path :variant) context)
        tincture (interface/get-raw-data (conj path :tincture) context)
        drop-article? (get-in context [:blazonry :drop-article?])
        part-of-charge-group? (get-in context [:blazonry :part-of-charge-group?])
        pluralize? (get-in context [:blazonry :pluralize?])
        context (-> context
                    (update :blazonry dissoc :drop-article?)
                    (update :blazonry dissoc :part-of-charge-group?)
                    (update :blazonry dissoc :pluralize?))
        charge-data (when variant
                      (if load-charge-data
                        (load-charge-data variant)
                        (interface/fetch-charge-data :frontend variant context)))
        fixed-tincture (-> charge-data
                           :fixed-tincture
                           (or :none)
                           (#(when (not= :none %) %)))
        charge-name (util/translate charge-type)]
    (util/combine " " [(when (and (not part-of-charge-group?)
                                  (not drop-article?))
                         "a")
                       (str charge-name
                            (when pluralize?
                              (if (s/ends-with? charge-name "s")
                                "es"
                                "s")))
                       (when-not (= attitude :none)
                         (util/translate attitude))
                       (when-not (#{:none :to-dexter} facing)
                         (util/translate facing))
                       (if fixed-tincture
                         (util/translate fixed-tincture)
                         (interface/blazon (conj path :field) context))
                       (util/combine
                        " and "
                        (map (fn [colour-key]
                               (when-let [t (get tincture colour-key)]
                                 (when (not= t :none)
                                   (util/combine " " [(util/translate colour-key)
                                                      (util/translate t)]))))
                             (-> attributes/tincture-modifier-map
                                 keys
                                 sort)))])))
