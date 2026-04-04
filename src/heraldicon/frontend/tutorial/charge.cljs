(ns heraldicon.frontend.tutorial.charge
  (:require
   [heraldicon.config :as config]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.library.charge.shared :refer [entity-type]]
   [heraldicon.frontend.tutorial :as-alias tutorial]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(def goals
  [{:title :string.tutorial.charge/welcome-title
    :description :string.tutorial.charge/welcome-description}

   {:title :string.tutorial.charge/upload-title
    :description :string.tutorial.charge/upload-description
    :hints [{:element "[data-tour='charge-upload']"
             :side "top"}]}

   {:title :string.tutorial.charge/original-preview-title
    :description :string.tutorial.charge/original-preview-description
    :hints [{:element "[data-tour='charge-original-preview']"
             :side "right"}]}

   {:title :string.tutorial.charge/charge-preview-title
    :description :string.tutorial.charge/charge-preview-description
    :hints [{:element "[data-tour='charge-preview']"
             :side "left"}]}

   {:title :string.tutorial.charge/charge-type-title
    :description :string.tutorial.charge/charge-type-description
    :hints [{:element "[data-tour='charge-type-select']"
             :side "left"}]}

   {:title :string.tutorial.charge/colours-title
    :description :string.tutorial.charge/colours-description
    :hints [{:element "[data-tour='charge-colours']"
             :side "left"}]}

   {:title :string.tutorial.charge/save-title
    :description :string.tutorial.charge/save-description
    :hints [{:element "[data-tour='save-button']"
             :side "top"}]}

   {:title :string.tutorial.charge/finish-title
    :description :string.tutorial.charge/finish-description}])

(def ^:private tree-identifier :heraldicon.frontend.library.charge.details/identifier)

(rf/reg-event-fx ::start
  (fn [{:keys [db]} _]
    (let [charge-id (id/for-url (config/get :tutorial-charge-editor-example-charge-id))]
      (reife/push-state :route.charge.details/by-id {:id charge-id})
      {:db (-> db
               (update :forms dissoc entity-type)
               (tree/clear tree-identifier))
       :dispatch [::tutorial/start :charge]})))
