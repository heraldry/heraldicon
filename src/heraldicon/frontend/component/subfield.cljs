(ns heraldicon.frontend.component.subfield
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.field.core :as field]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(macros/reg-event-fx ::override-part-reference
  (fn [{:keys [db]} [_ path]]
    (let [{:keys [index]} (get-in db path)
          referenced-part (get-in db (-> path
                                         drop-last
                                         vec
                                         (conj index)))]
      {:db (assoc-in db path referenced-part)
       :dispatch [::tree/select-node path true]})))

(macros/reg-event-db ::reset-part-reference
  (fn [db [_ {:keys [path] :as context}]]
    (let [index (last path)
          parent-context (c/-- context 2)
          default-fields (field/default-fields parent-context)]
      (assoc-in db path (get default-fields index)))))

(defn- parent-context [{:keys [path] :as context}]
  (let [index (last path)
        parent-context (c/-- context 2)
        parent-type (interface/get-raw-data (c/++ parent-context :type))]
    (when (and (int? index)
               (-> parent-type (or :dummy) namespace (= "heraldry.field.type")))
      parent-context)))

(defn- non-mandatory-part-of-parent? [{:keys [path] :as context}]
  (let [index (last path)]
    (when (int? index)
      (when-let [parent-context (parent-context context)]
        (>= index (field/mandatory-part-count parent-context))))))

;; TODO: this should all be refactored
(defn- name-prefix-for-part [{:keys [path] :as context}]
  (when-let [parent-context (parent-context context)]
    (let [parent-type (interface/get-raw-data (c/++ parent-context :type))]
      (string/upper-case-first (field/part-name parent-type (last path))))))

(defmethod component/node :heraldry/subfield [{:keys [path] :as context}]
  (let [subfield-type (interface/get-raw-data (c/++ context :type))
        node-data (if (= subfield-type :heraldry.subfield.type/reference)
                    (let [reference-index (interface/get-raw-data (c/++ context :index))]
                      {:title (string/str-tr :string.miscellaneous/field-reference
                                             " "
                                             (name-prefix-for-part
                                              (-> context c/-- (c/++ reference-index))))
                       :icon {:default [:span {:style {:display "inline-block"}}]
                              :selected [:span {:style {:display "inline-block"}}]}
                       :buttons [{:icon "fas fa-sliders-h"
                                  :title :string.user.button/change
                                  :handler #(rf/dispatch [::override-part-reference path])}]})
                    (cond-> (component/node (c/++ context :field))
                      (non-mandatory-part-of-parent? context)
                      (update :buttons conj {:icon "fas fa-undo"
                                             :title "Reset"
                                             :handler #(rf/dispatch [::reset-part-reference context])})))]
    (update node-data :title (fn [title]
                               (string/str-tr (name-prefix-for-part context) ": " title)))))

(defmethod component/form :heraldry/subfield [context]
  (let [subfield-type (interface/get-raw-data (c/++ context :type))]
    ;; TODO: add form for refs
    (when (= subfield-type :heraldry.subfield.type/field)
      {:form (component/form (c/++ context :field))
       :effective-context (c/++ context :field)})))
