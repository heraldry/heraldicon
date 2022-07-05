(ns heraldicon.frontend.entity.action.save
  (:require
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn button [entity-type]
  (let [form-db-path (form/data-path entity-type)
        can-save? (and @(rf/subscribe [::session/logged-in?])
                       (or (not @(rf/subscribe [::entity/saved? form-db-path]))
                           @(rf/subscribe [::entity/owned-by? form-db-path @(rf/subscribe [::session/data])])
                           @(rf/subscribe [::session/admin?])))]

    [:button.button.primary {:type "submit"
                             :class (when-not can-save? "disabled")
                             :title (when-not can-save?
                                      :string.user.message/need-to-be-logged-in-and-own-the-arms)
                             :on-click (when can-save?
                                         (fn [event]
                                           (.preventDefault event)
                                           (.stopPropagation event)
                                           (rf/dispatch [::details/save entity-type])))
                             :style {:flex "initial"
                                     :margin-left "10px"}}
     [tr :string.button/save]]))
