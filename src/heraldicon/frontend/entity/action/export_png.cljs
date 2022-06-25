(ns heraldicon.frontend.entity.action.export-png
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

(defn- generate-png-api-function [entity-type]
  (case entity-type
    :heraldicon.entity.type/arms :generate-png-arms
    :heraldicon.entity.type/collection :generate-png-collection))

(defn- invoke [entity-type]
  (modal/start-loading)
  (go
    (try
      (let [form-db-path (form/data-path entity-type)
            full-data @(rf/subscribe [:get form-db-path])
            ;; TODO: this can probably live elsewhere
            payload (-> full-data
                        (select-keys [:id :version])
                        (assoc :render-options
                               (get-in full-data
                                       (case entity-type
                                         :heraldicon.entity.type/arms [:data :achievement :render-options]
                                         :heraldicon.entity.type/collection [:data :render-options]))))
            session @(rf/subscribe [::session/data])
            response (<? (api/call (generate-png-api-function entity-type) payload session))]
        (js/window.open (:png-url response)))

      (catch :default e
        (log/error "generate png arms error:" e))

      (finally
        (modal/stop-loading)))))

(defn action [entity-type]
  (when (#{:heraldicon.entity.type/arms
           :heraldicon.entity.type/collection} entity-type)
    (let [form-db-path (form/data-path entity-type)
          logged-in? @(rf/subscribe [::session/logged-in?])
          can-export? (and logged-in?
                           @(rf/subscribe [::entity/saved? form-db-path])
                           (not @(rf/subscribe [::form/unsaved-changes? entity-type])))]
      {:title (string/str-tr :string.button/export " (PNG)")
       :icon "fas fa-file-export"
       :handler (when can-export?
                  (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (invoke entity-type)))
       :disabled? (not can-export?)
       :tooltip (when-not can-export?
                  (if (not logged-in?)
                    :string.user.message/need-to-be-logged-in
                    :string.user.message/save-changes-first))})))
