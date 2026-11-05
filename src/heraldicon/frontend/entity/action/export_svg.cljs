(ns heraldicon.frontend.entity.action.export-svg
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.entity.action.export-download :as export-download]
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.export :as export]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(rf/reg-fx ::export-svg
  (fn [[entity-type render-data clip?]]
    (modal/start-loading)
    (go
      (try
        (let [svg-data (<? (export/render-svg entity-type render-data
                                              :clip? clip?
                                              :add-url? (= entity-type :heraldicon.entity.type/collection)))]
          (export-download/trigger-blob (js/Blob. #js [svg-data] #js {:type "image/svg+xml"})
                                        (export/filename entity-type (:entity-data render-data)
                                                         (if clip?
                                                           " (clips).svg"
                                                           " (masks).svg"))))

        (catch :default e
          (log/error e "generate svg error"))

        (finally
          (modal/stop-loading))))))

(rf/reg-event-fx ::invoke
  (fn [_ [_ entity-type render-data clip?]]
    {::export-svg [entity-type render-data clip?]}))

(defn action [entity-type & {:keys [clip?]}]
  (when (#{:heraldicon.entity.type/arms
           :heraldicon.entity.type/collection} entity-type)
    (let [form-db-path (form/data-path entity-type)
          logged-in? @(rf/subscribe [::session/logged-in?])
          can-export? (and logged-in?
                           @(rf/subscribe [::entity/saved? form-db-path])
                           (not @(rf/subscribe [::form/unsaved-changes? entity-type])))
          render-data (when can-export?
                        (export/render-data entity-type @(rf/subscribe [:get form-db-path])))]
      {:title (string/str-tr :string.button/export (if clip?
                                                     " (SVG, clips)"
                                                     " (SVG, masks)"))
       :icon "fas fa-file-export"
       :handler (when render-data
                  #(rf/dispatch [::invoke entity-type render-data clip?]))
       :disabled? (not render-data)
       :tooltip (when-not can-export?
                  (if (not logged-in?)
                    :string.user.message/need-to-be-logged-in
                    :string.user.message/save-changes-first))})))
