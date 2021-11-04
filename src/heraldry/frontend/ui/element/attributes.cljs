(ns heraldry.frontend.ui.element.attributes
  (:require
   [heraldry.coat-of-arms.attributes :as attributes]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(macros/reg-event-db :add-attribute
  (fn [db [_ db-path attribute]]
    (update-in db db-path assoc attribute true)))

(macros/reg-event-db :remove-attribute
  (fn [db [_ db-path attribute]]
    (update-in db db-path dissoc attribute)))

(defn attribute-view [attribute & {:keys [on-delete]}]
  [:span.tag.attribute
   (name attribute)
   (when on-delete
     [:span.delete {:on-click on-delete}
      "x"])])

(defn attributes-view [attributes & {:keys [on-delete
                                            on-click
                                            selected]}]
  [:div.attributes
   (doall
    (for [attribute (sort attributes)]
      ^{:key attribute}
      [:<>
       [attribute-view attribute
        :on-delete (when on-delete
                     #(on-delete attribute))
        :on-click (when on-click
                    #(on-click attribute))
        :selected? (get selected attribute)]
       " "]))])

(defn form [{:keys [path] :as context}]
  (let [attributes (interface/get-raw-data context)]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"}}
      [:label [tr {:en "Attributes"
                   :de "Attribute"}]]
      [:div.option
       [:select {:on-change #(let [selected (keyword (-> % .-target .-value))]
                               (rf/dispatch [:add-attribute path selected]))
                 :value :none}
        (doall
         (for [[group-name & group-choices] (concat
                                             [[{:en "--- Add attribute ---"
                                                :de "--- Attribut hinzufügen ---"} :none]]
                                             attributes/attribute-choices)]
           (if (and (-> group-choices count (= 1))
                    (-> group-choices first keyword?))
             (let [key (-> group-choices first)]
               ^{:key key}
               [:option {:value (util/keyword->str key)}
                (tr group-name)])
             ^{:key group-name}
             [:optgroup {:label (tr group-name)}
              (doall
               (for [[display-name key] group-choices]
                 ^{:key key}
                 [:option {:value (util/keyword->str key)}
                  (tr display-name)]))])))]
       [:div {:style {:padding-top "10px"}}
        [attributes-view (keys attributes)
         :on-delete #(rf/dispatch [:remove-attribute path %])]]]]]))

(defmethod ui-interface/form-element :attributes [context]
  [form context])
