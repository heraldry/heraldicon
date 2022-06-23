(ns heraldicon.frontend.user.form.core
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [re-frame.core :as rf]))

(def ^:private db-path-user-forms
  [:ui :user-form :forms])

(defn- form-path [form-id]
  (conj db-path-user-forms form-id))

(defn- form-field-path [form-id field-id]
  (conj (form-path form-id) field-id))

(rf/reg-sub ::field-value
  (fn [[_ form-id field-id] _]
    (rf/subscribe [:get (form-field-path form-id field-id)]))

  (fn [value [_ _form-id _field-id]]
    value))

(rf/reg-event-db ::set-field-value
  (fn [db [_ form-id field-id value]]
    (assoc-in db (form-field-path form-id field-id) value)))

(rf/reg-event-fx ::clear
  (fn [{:keys [db]} [_ form-id]]
    {:db (assoc-in db (form-path form-id) nil)
     :dispatch [::message/clear form-id]}))

(rf/reg-event-fx ::clear-and-close
  (fn [_ [_ form-id]]
    {:dispatch-n [[::modal/clear]
                  [::clear form-id]]}))

(defn data-from-db [db form-id]
  (get-in db (form-path form-id)))

(defn message-id [form-id field-id]
  {:parent form-id
   :id field-id})

(defn text-field [form-id field-id placeholder & {:keys [type]
                                                  :or {type "text"}}]
  (let [message-id (message-id form-id field-id)]
    [:div {:class (when @(rf/subscribe [::message/error? message-id])
                    "error")}
     [message/display message-id]
     [:div
      [:input {:id (name field-id)
               :name (name field-id)
               :value @(rf/subscribe [::field-value form-id field-id])
               :on-change #(let [new-value (-> % .-target .-value)]
                             (rf/dispatch-sync [::set-field-value form-id field-id new-value]))
               :placeholder (tr placeholder)
               :type type}]]]))

(defn password-field [form-id field-id placeholder]
  (text-field form-id field-id placeholder :type "password"))

(defn on-submit-fn [event-vector]
  (fn [event]
    (doto event
      .preventDefault
      .stopPropagation)
    (rf/dispatch event-vector)))
