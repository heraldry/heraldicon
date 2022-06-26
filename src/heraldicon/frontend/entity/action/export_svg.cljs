(ns heraldicon.frontend.entity.action.export-svg
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.api :as api]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn- generate-svg-api-function [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :generate-svg-arms
    :heraldicon.entity.type/collection :generate-svg-collection))

(rf/reg-fx ::export-svg
  (fn [[entity-type entity-data session]]
    (modal/start-loading)
    (go
      (try
        (let [;; TODO: this can probably live elsewhere
              payload (-> entity-data
                          (select-keys [:id :version])
                          (assoc :render-options
                                 (get-in entity-data
                                         (case entity-type
                                           :heraldicon.entity.type/arms [:data :achievement :render-options]
                                           :heraldicon.entity.type/collection [:data :render-options]))))
              response (<? (api/call (generate-svg-api-function entity-type) payload session))]
          (js/window.open (:svg-url response)))

        (catch :default e
          (log/error "generate svg error:" e))

        (finally
          (modal/stop-loading))))))

(rf/reg-event-fx ::invoke
  (fn [{:keys [db]} [_ entity-type session]]
    (let [form-db-path (form/data-path entity-type)
          entity-data (get-in db form-db-path)]
      {::export-svg [entity-type entity-data session]})))

(defn action [entity-type]
  (when (#{:heraldicon.entity.type/arms
           :heraldicon.entity.type/collection} entity-type)
    (let [form-db-path (form/data-path entity-type)
          logged-in? @(rf/subscribe [::session/logged-in?])
          can-export? (and logged-in?
                           @(rf/subscribe [::entity/saved? form-db-path])
                           (not @(rf/subscribe [::form/unsaved-changes? entity-type])))
          session @(rf/subscribe [::session/data])]
      {:title (string/str-tr :string.button/export " (SVG)")
       :icon "fas fa-file-export"
       :handler (when can-export?
                  #(rf/dispatch [::invoke entity-type session]))
       :disabled? (not can-export?)
       :tooltip (when-not can-export?
                  (if (not logged-in?)
                    :string.user.message/need-to-be-logged-in
                    :string.user.message/save-changes-first))})))
