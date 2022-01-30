(ns heraldry.frontend.ui.element.tags
  (:require
   [clojure.string :as s]
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [re-frame.core :as rf]))

(def value-path [:ui :tag-input-value])

(defn normalize-tag [tag]
  (let [normalized-tag (-> tag
                           (cond->
                             (keyword? tag) name)
                           (or "")
                           s/trim
                           s/lower-case)]
    (when (-> normalized-tag
              count
              pos?)
      (keyword normalized-tag))))

(macros/reg-event-db :add-tags
  (fn [db [_ db-path tags]]
    (update-in db db-path (fn [current-tags]
                            (-> current-tags
                                keys
                                set
                                (concat tags)
                                (->> (map normalize-tag)
                                     (filter identity)
                                     set
                                     (map (fn [tag]
                                            [tag true]))
                                     (into {})))))))

(macros/reg-event-db :remove-tags
  (fn [db [_ db-path tags]]
    (update-in db db-path (fn [current-tags]
                            (loop [current-tags current-tags
                                   [tag & remaining] (->> tags
                                                          (map normalize-tag)
                                                          (filter identity)
                                                          set)]
                              (if tag
                                (recur (dissoc current-tags tag)
                                       remaining)
                                current-tags))))))

(defn on-change [event]
  (let [new-value (-> event .-target .-value)]
    (rf/dispatch-sync [:set value-path new-value])))

(defn add-tag-clicked [path value]
  (let [tags (-> value
                 (or "")
                 s/trim
                 s/lower-case
                 (s/split #"[^a-z0-9-]+")
                 (->> (filter #(-> % count pos?))))]
    (rf/dispatch [:add-tags path tags])
    (rf/dispatch [:set value-path nil])))

(defn delete-tag-clicked [path tag]
  (rf/dispatch [:remove-tags path [tag]]))

(defn tag-view [tag & {:keys [on-delete
                              on-click
                              selected?]}]
  [:span.tag {:style {:background (if selected?
                                    "#f2bc51"
                                    "#0c6793")
                      :color (if selected?
                               "#000"
                               "#eee")
                      :cursor (when on-click
                                "pointer")}
              :on-click on-click}
   (name tag)
   (when on-delete
     [:span.delete {:on-click on-delete}
      "x"])])

(defn tags-view [tags & {:keys [on-delete
                                on-click
                                selected]}]
  (let [sorted-tags (if (seq? tags)
                      (sort tags)
                      (sort-by
                       (fn [tag]
                         [(get selected tag)
                          (get tags tag)])
                       #(compare %2 %1)(keys tags)))]
    [:div.tags
     (for [tag sorted-tags]
       ^{:key tag}
       [:<>
        [tag-view (if (seq? tags)
                    tag
                    (str (name tag) ": " (get tags tag)))
         :on-delete (when on-delete
                      #(on-delete tag))
         :on-click (when on-click
                     #(on-click tag))
         :selected? (get selected tag)]])]))

(defn form [{:keys [path] :as context}]
  (let [value (interface/get-raw-data (c/<< context :path value-path))
        tags (interface/get-raw-data context)
        on-click (fn [event]
                   (.preventDefault event)
                   (.stopPropagation event)
                   (add-tag-clicked path value))]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"}}
      [:label [tr :string.entity/tags]]
      [:div.option
       [:input {:value value
                :on-change on-change
                :on-key-press (fn [event]
                                (when (-> event .-code (= "Enter"))
                                  (on-click event)))
                :type "text"
                :style {:margin-right "0.5em"}}]
       [:button
        {:disabled (-> value (or "") s/trim count zero?)
         :on-click on-click
         :type "button"}
        [tr :string.button/add]]
       [:div {:style {:padding-top "10px"}}
        [tags-view (keys tags)
         :on-delete #(delete-tag-clicked path %)]]]]]))

(defmethod ui-interface/form-element :tags [context]
  [form context])
