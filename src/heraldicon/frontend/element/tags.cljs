(ns heraldicon.frontend.element.tags
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.entity.tag :as tag]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(def ^:private value-path
  [:ui :tag-input-value])

(macros/reg-event-db ::add
  (fn [db [_ db-path tags]]
    (update-in db db-path (fn [current-tags]
                            (into {}
                                  (map (fn [tag]
                                         [tag true]))
                                  (->> current-tags
                                       keys
                                       (concat tags)
                                       (keep tag/clean)))))))

(macros/reg-event-db ::remove
  (fn [db [_ db-path tags]]
    (update-in db db-path (fn [current-tags]
                            (loop [current-tags current-tags
                                   [tag & remaining] (set (keep tag/clean tags))]
                              (if tag
                                (recur (dissoc current-tags tag)
                                       remaining)
                                current-tags))))))

(defn- on-change [event]
  (let [new-value (-> event .-target .-value)]
    (rf/dispatch-sync [:set value-path new-value])))

(defn- add-tag-clicked [path value]
  (let [tags (-> value
                 (or "")
                 str/trim
                 str/lower-case
                 (str/split #"[^a-z0-9-]+")
                 (->> (keep tag/clean)))]
    (rf/dispatch [::add path tags])
    (rf/dispatch [:set value-path nil])))

(defn- delete-tag-clicked [path tag]
  (rf/dispatch [::remove path [tag]]))

(defn- tag-view [tag & {:keys [on-delete
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
                                selected
                                style]}]
  (let [sorted-tags (if (map? tags)
                      (sort-by (fn [tag]
                                 [(get selected tag)
                                  (get tags tag)])
                               #(compare %2 %1)
                               (set (concat (keys tags)
                                            (keep (fn [[k v]]
                                                    (when v
                                                      k))
                                                  selected))))
                      (sort tags))]
    (into [:div.tags {:style style}]
          (map (fn [tag]
                 ^{:key tag}
                 [tag-view (if (map? tags)
                             (str (name tag) ": " (or (get tags tag) 0))
                             tag)
                  :on-delete (when on-delete
                               #(on-delete tag))
                  :on-click (when on-click
                              #(on-click tag))
                  :selected? (get selected tag)]))
          sorted-tags)))

(defmethod element/element :ui.element/tags [{:keys [path] :as context}]
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
        {:disabled (str/blank? value)
         :on-click on-click
         :type "button"}
        [tr :string.button/add]]
       [:div {:style {:padding-top "10px"}}
        [tags-view (keys tags)
         :on-delete #(delete-tag-clicked path %)]]]]]))
