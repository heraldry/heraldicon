(ns heraldry.frontend.ui.element.tags
  (:require [clojure.string :as s]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.macros :as macros]
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
  [:div.tags
   (for [tag (sort tags)]
     ^{:key tag}
     [:<>
      [tag-view tag
       :on-delete (when on-delete
                    #(on-delete tag))
       :on-click (when on-click
                   #(on-click tag))
       :selected? (get selected tag)]
      " "])])

(defn form [path]
  (let [value @(rf/subscribe [:get-value value-path])
        tags @(rf/subscribe [:get-value path])
        on-click (fn [event]
                   (.preventDefault event)
                   (.stopPropagation event)
                   (add-tag-clicked path value))]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"}}
      [:label "Tags"]
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
        "Add"]
       [:div {:style {:padding-top "10px"}}
        [tags-view (keys tags)
         :on-delete #(delete-tag-clicked path %)]]]]]))

(defmethod interface/form-element :tags [path]
  [form path])
