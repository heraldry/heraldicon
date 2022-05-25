(ns heraldicon.frontend.ui.element.attributes
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.util.core :as util]
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
  (into [:div.attributes]
        (map (fn [attribute]
               ^{:key attribute}
               [:<>
                [attribute-view attribute
                 :on-delete (when on-delete
                              #(on-delete attribute))
                 :on-click (when on-click
                             #(on-click attribute))
                 :selected? (get selected attribute)]
                " "]))
        (sort attributes)))

(defn form [{:keys [path] :as context}]
  (let [attributes (interface/get-raw-data context)]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"}}
      [:label [tr :string.entity/attributes]]
      [:div.option
       (into [:select {:on-change #(let [selected (keyword (-> % .-target .-value))]
                                     (rf/dispatch [:add-attribute path selected]))
                       :value :none}]
             (map (fn [[group-name & group-choices]]
                    (if (and (-> group-choices count (= 1))
                             (-> group-choices first keyword?))
                      (let [key (first group-choices)]
                        ^{:key key}
                        [:option {:value (util/keyword->str key)}
                         (tr group-name)])
                      (into
                       ^{:key group-name}
                       [:optgroup {:label (tr group-name)}]
                       (map (fn [[display-name key]]
                              ^{:key key}
                              [:option {:value (util/keyword->str key)}
                               (tr display-name)]))
                       group-choices))))
             (concat [[(string/str-tr "--- " :string.charge.attribute.ui/add " ---") :none]]
                     attributes/attribute-choices))
       [:div {:style {:padding-top "10px"}}
        [attributes-view (keys attributes)
         :on-delete #(rf/dispatch [:remove-attribute path %])]]]]]))

(defmethod ui.interface/form-element :attributes [context]
  [form context])
