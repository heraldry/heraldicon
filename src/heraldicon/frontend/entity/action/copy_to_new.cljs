(ns heraldicon.frontend.entity.action.copy-to-new
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.message :as message]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn- invoke [form-id]
  (let [form-db-path (form/data-path form-id)]
    (rf/dispatch-sync [::message/clear form-id])
    ;; TODO: implement the copy
    (rf/dispatch-sync [::message/set-success form-id :string.user.message/created-unsaved-copy])
    (reife/push-state :route.arms/create)))

(defn action [form-id]
  {:title :string.button/copy-to-new
   :icon "fas fa-clone"
   :handler (partial invoke form-id)
   :disabled? false})
