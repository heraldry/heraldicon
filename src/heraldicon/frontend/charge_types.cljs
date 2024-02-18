(ns heraldicon.frontend.charge-types
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.context :as c]
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.repository.charge-types :as repository.charge-types]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.form.core :as form]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private form-db-path
  (form/form-path ::form))

(history/register-undoable-path form-db-path)

(def base-context
  (c/<< context/default :path form-db-path))

(rf/reg-event-fx ::save
  (fn [{:keys [db]} _]
    (let [session (session/data-from-db db)
          data (get-in db form-db-path)]
      (go
        (try
          (let [result (<? (api/call :save-charge-types data session))]
            (rf/dispatch [:set form-db-path result])
            (rf/dispatch-sync [::message/set-success ::id "Updated"])
            (js/setTimeout #(rf/dispatch [::message/clear ::id]) 3000))

          (catch :default error
            (rf/dispatch [::message/set-error ::id (:message (ex-data error))])
            (log/error error "save charge-types error"))))

      {})))

(defn- charge-type-editor
  []
  (rf/dispatch [::title/set :string.menu/charge-types])
  (status/default
   (rf/subscribe [::repository.charge-types/data #(rf/dispatch [:set form-db-path %])])
   (fn [_]
     [:div {:style {:position "relative"
                    :max-width "40em"
                    :height "calc(100vh - 4em)"
                    :padding "10px"}}
      [:div {:style {:height "calc(100% - 2.5em)"
                     :overflow "scroll"}}
       [history/buttons form-db-path]
       [tree/tree [form-db-path] base-context]]

      [:button.button.primary {:type "submit"
                               :on-click (fn [event]
                                           (.preventDefault event)
                                           (.stopPropagation event)
                                           (rf/dispatch [::save]))
                               :style {:float "right"
                                       :margin-bottom "10px"}}
       [tr :string.button/save]]
      [:div {:style {:width "80%"
                     :float "left"}}
       [message/display ::id]]])))

(defn view []
  (if (entity.user/admin? @(rf/subscribe [::session/data]))
    [charge-type-editor]
    [:div]))
