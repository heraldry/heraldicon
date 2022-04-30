(ns heraldicon.heraldry.charge.core
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.interface :as interface]
   [heraldicon.translation.string :as string]
   [heraldicon.util :as util]))

(defmethod interface/render-component :heraldry.component/charge [context]
  [charge.interface/render-charge context])

(defmethod interface/blazon-component :heraldry.component/charge [{:keys [load-charge-data] :as context}]
  (let [charge-type (interface/get-raw-data (c/++ context :type))
        attitude (interface/get-sanitized-data (c/++ context :attitude))
        facing (interface/get-sanitized-data (c/++ context :facing))
        variant (interface/get-raw-data (c/++ context :variant))
        tincture (interface/get-raw-data (c/++ context :tincture))
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
        charge-name (util/translate charge-type)
        pluralize? (and pluralize?
                        (not (#{"fleur-de-lis"} charge-name)))]
    (string/combine " " [(when (and (not part-of-charge-group?)
                                    (not drop-article?))
                           "a")
                         (string/str-tr charge-name
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
                           (interface/blazon (c/++ context :field)))
                         (string/combine
                          " and "
                          (map (fn [colour-key]
                                 (when-let [t (get tincture colour-key)]
                                   (when (not= t :none)
                                     (string/combine " " [(util/translate colour-key)
                                                          (util/translate t)]))))
                               (-> attributes/tincture-modifier-map
                                   keys
                                   sort)))
                         (fimbriation/blazon context)])))
