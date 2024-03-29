(ns heraldicon.heraldry.charge.core
  (:require
   [clojure.string :as str]
   [heraldicon.blazonry :as blazonry]
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]))

(defmethod interface/blazon-component :heraldry/charge [{:keys [load-charge-data] :as context}]
  (let [attitude (interface/get-sanitized-data (c/++ context :attitude))
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
                      (:entity (load-charge-data variant)))
        charge-type (or (-> charge-data
                            :data
                            :charge-type)
                        (interface/get-raw-data (c/++ context :type)))
        fixed-tincture (-> charge-data
                           :fixed-tincture
                           (or :none)
                           (#(when (not= :none %) %)))
        charge-name (blazonry/translate charge-type)
        pluralize? (and pluralize?
                        (not (#{"fleur-de-lis"} charge-name)))]
    (string/combine " " [(when (and (not part-of-charge-group?)
                                    (not drop-article?))
                           "a")
                         (string/str-tr charge-name
                                        (when pluralize?
                                          (if (str/ends-with? charge-name "s")
                                            "es"
                                            "s")))
                         (when-not (= attitude :none)
                           (blazonry/translate attitude))
                         (when-not (#{:none :to-dexter} facing)
                           (blazonry/translate facing))
                         (if fixed-tincture
                           (blazonry/translate fixed-tincture)
                           (interface/blazon (c/++ context :field)))
                         (string/combine
                          " and "
                          (map (fn [colour-key]
                                 (when-let [t (get tincture colour-key)]
                                   (when (not= t :none)
                                     (string/combine " " [(blazonry/translate colour-key)
                                                          (blazonry/translate t)]))))
                               (-> attributes/tincture-modifier-map
                                   keys
                                   sort)))
                         (fimbriation/blazon context)])))
