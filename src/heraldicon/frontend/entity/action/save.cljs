(ns heraldicon.frontend.entity.action.save
  (:require
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]))

(defn- invoke [form-id]
  (details/save form-id))

(defn button [form-id]
  (let [form-db-path (form/data-path form-id)
        can-save? (and @(rf/subscribe [::user/logged-in?])
                       (or (not @(rf/subscribe [::entity/saved? form-db-path]))
                           @(rf/subscribe [::entity/owned-by? form-db-path (user/data)])))]

    [:button.button.primary {:type "submit"
                             :class (when-not can-save? "disabled")
                             :title (when-not can-save?
                                      :string.user.message/need-to-be-logged-in-and-own-the-arms)
                             :on-click (when can-save?
                                         (fn [event]
                                           (.preventDefault event)
                                           (.stopPropagation event)
                                           (invoke form-id)))
                             :style {:flex "initial"
                                     :margin-left "10px"}}
     [tr :string.button/save]]))
