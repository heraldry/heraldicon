(ns heraldicon.export.arms
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.arms :as entity.arms]
   [heraldicon.export.core :as export]
   [heraldicon.heraldry.achievement :as achievement]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]))

(def short-url-font
  :deja-vu-sans)

(defn- prepare-context
  "Stores the render data in the bound app-db and returns the context of the
  achievement."
  [{:keys [entity-data charge-data-map]} override-render-options-path]
  (let [unique-path (export/unique-path entity-data)
        achievement-path (conj unique-path :data :achievement)
        charge-data-path (conj unique-path :charge-data-path)]
    (export/set-data! unique-path entity-data)
    (doseq [[variant charge-data] charge-data-map]
      (export/set-data! (conj charge-data-path variant) charge-data))
    {:path achievement-path
     :render-options-path (conj achievement-path :render-options)
     :override-render-options-path override-render-options-path
     :load-charge-data (fn [variant]
                         (let [charge-path (conj charge-data-path variant)]
                           {:status :done
                            :entity (interface/get-raw-data {:path charge-path})
                            :path charge-path}))}))

(defn used-fonts
  "The fonts render-svg embeds. Call it inside export/with-render-state."
  [render-data & {:keys [add-short-url?]}]
  (cond-> (achievement/used-fonts (prepare-context render-data nil))
    add-short-url? (conj short-url-font)))

(defn render-svg
  "Renders arms synchronously. Call it inside export/with-render-state, which
  can span several renders that share one document.

  charge-data-map maps each charge variant to its charge entity, with the
  :edn-data loaded. texture-link is the texture as a data URI, if the arms
  use one. font-data-uris maps a font to its data URI, for embed-fonts?."
  [{:keys [entity-data texture-link] :as render-data}
   & {:keys [add-short-url?
             target-width
             target-height
             hiccup?
             reset-ids?
             override-render-options-path
             embed-fonts?
             font-data-uris
             clip?]
      :or {reset-ids? true
           embed-fonts? true}}]
  (when reset-ids?
    (uid/reset))
  (let [context (-> (prepare-context render-data override-render-options-path)
                    (c/set-render-hint :svg-export? true
                                       :clip? clip?
                                       :embed-fonts (when embed-fonts?
                                                      (partial export/embed-fonts font-data-uris))
                                       :root-path (export/unique-path entity-data)
                                       :texture-link texture-link
                                       :short-url (when add-short-url?
                                                    (some->
                                                     entity-data
                                                     entity.arms/short-url))
                                       :target-width target-width
                                       :target-height target-height))
        svg-data (interface/render-component context)]
    (if hiccup?
      svg-data
      (export/svg-markup svg-data))))
