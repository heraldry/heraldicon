(ns heraldry.frontend.form.tag
  (:require [clojure.string :as s]
            [re-frame.core :as rf]))

(def value-path [:ui :tag-input-value])

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

(defn tag-view [tag & {:keys [on-delete]}]
  [:span.tag {:style {:background "#0c6793"
                      :color "#eee"}}
   tag
   (when on-delete
     [:span.delete {:on-click #(on-delete)}
      "x"])])

(defn form [path]
  (let [value @(rf/subscribe [:get value-path])
        tags @(rf/subscribe [:get path])
        on-click (fn [event]
                   (.preventDefault event)
                   (.stopPropagation event)
                   (add-tag-clicked path value))]
    [:<>
     [:div.pure-control-group
      [:label {:for   "name"
               :style {:width "6em"}} "Tag"]
      [:input {:id        "name"
               :value     value
               :on-change on-change
               :on-key-press (fn [event]
                               (when (-> event .-code (= "Enter"))
                                 (on-click event)))
               :type      "text"
               :style     {:margin-right "0.5em"}}]
      [:button
       {:disabled (-> value (or "") s/trim count zero?)
        :on-click on-click
        :type "button"}
       "Add"]
      [:div.tags
       (for [tag (sort (keys tags))]
         ^{:key tag}
         [:<>
          [tag-view tag
           :on-delete #(delete-tag-clicked path tag)]
          " "])]]]))
