(ns heraldicon.frontend.component.tree
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as frontend.component]
   [heraldicon.frontend.component.entity.collection.element :as collection.element]
   [heraldicon.frontend.element.hover-menu :as hover-menu]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.heraldry.component :as component]
   [re-frame.core :as rf]))

(def node-context
  {:render-options {:type :heraldry/render-options}
   :render-options-path [:context :render-options]})

(defn node-data [{:keys [path] :as context}]
  (merge {:open? @(rf/subscribe [::node-open? path])
          :selected? (= path @(rf/subscribe [::active-node-path]))
          :selectable? true}
         (frontend.component/node context)))

(defn node [{:keys [path] :as context} & {:keys [title parent-buttons]}]
  (let [{node-title :title
         :keys [open?
                selected?
                selectable?
                nodes
                buttons
                annotation
                validation
                icon]} (node-data context)
        openable? (-> nodes count pos?)
        title (or node-title title)
        buttons (concat buttons parent-buttons)]
    [:<>
     [:div.node-name.clickable.no-select
      {:class (str (when selected?
                     "selected ")
                   (when-not selectable?
                     "unselectable "))
       :on-click #(do
                    (when (or (not open?)
                              (not selectable?)
                              selected?)
                      (rf/dispatch [::toggle-node path]))
                    (when selectable?
                      (rf/dispatch [::select-node path]))
                    (.stopPropagation %))}
      (if openable?
        [:span.node-icon.clickable
         {:on-click (js-event/handled #(rf/dispatch [::toggle-node path]))
          :style {:width "0.9em"}}
         [:i.fa.ui-icon {:class (if open?
                                  "fa-angle-down"
                                  "fa-angle-right")}]]
        [:span.node-icon
         [:i.fa.ui-icon.fa-angle-down {:style {:opacity 0}}]])

      (when icon
        (let [effective-icon (if selected?
                               (:selected icon)
                               (:default icon))
              icon-style {:display "inline-block"
                          :width "14px"
                          :height "16px"
                          :margin-right "5px"
                          :vertical-align "top"
                          :transform "translate(0,3px)"}]
          (if (vector? effective-icon)
            (update-in effective-icon [1 :style] merge icon-style)
            [:div {:style icon-style}
             [:img {:src effective-icon
                    :style {:position "absolute"
                            :margin "auto"
                            :top 0
                            :left 0
                            :right 0
                            :bottom 0
                            :max-width "100%"
                            :max-height "100%"}}]])))

      [tr title]

      annotation

      (validation/render validation)

      (into [:<>]
            (comp (filter :menu)
                  (map-indexed (fn [idx {:keys [icon menu disabled? title]}]
                                 [hover-menu/hover-menu
                                  (c/++ context idx)
                                  title
                                  menu
                                  [:i.ui-icon {:class (str icon (when disabled?
                                                                  " disabled-item"))
                                               :title (tr title)
                                               :style {:margin-left "0.5em"
                                                       :font-size "0.8em"
                                                       :cursor (when disabled? "not-allowed")}}]
                                  :disabled? disabled?
                                  :require-click? true])))
            buttons)

      (into [:span {:style {:margin-left "0.5em"}}]
            (comp (remove :menu)
                  (map-indexed (fn [idx {:keys [icon handler disabled? title remove?]}]
                                 [:span.node-icon
                                  {:class (when disabled? "disabled")
                                   :title (tr title)
                                   :style {:margin-left (when (and (pos? idx)
                                                                   remove?) "0.5em")
                                           :cursor (when disabled? "not-allowed")}}
                                  [:i.ui-icon {:class (str icon (when disabled?
                                                                  " disabled-item"))
                                               :on-click (when-not disabled?
                                                           (js-event/handled handler))
                                               :style {:font-size "0.8em"}}]])))
            buttons)]

     (when open?
       (into [:ul]
             (map (fn [{:keys [context title buttons]}]
                    ^{:key context}
                    [:li [node context :title title :parent-buttons buttons]]))
             nodes))]))

(defn tree [paths context]
  (let [context (or context node-context)]
    [:div.ui-tree
     (into [:ul]
           (map-indexed (fn [idx node-path]
                          ^{:key idx}
                          [:li
                           (if (= node-path :spacer)
                             [:div {:style {:height "1em"}}]
                             [node (c/<< context :path node-path)])]))
           paths)]))

(def ^:private active-node-path
  [:ui :component-tree :selected-node])

(def ^:private node-selected-default-path
  [:ui :component-tree :selected-node-default])

(def ^:private node-flag-db-path
  [:ui :component-tree :nodes])

(defn- node-open-by-default? [path]
  (or (#{[:coat-of-arms]
         [:helms]
         [:ornaments]
         [:elements]}
       (take-last 1 path))
      (#{[:coat-of-arms :field]}
       (take-last 2 path))
      (#{;; TODO: path shouldn't be hard-coded
         [:heraldicon.entity/collection :data :data]}
       (take-last 3 path))
      (#{[:example-coa :coat-of-arms :field :components 0]}
       (take-last 5 path))))

(defn- node-open? [flag path]
  (if (nil? flag)
    (node-open-by-default? path)
    flag))

(rf/reg-sub ::node-open?
  (fn [[_ path] _]
    (rf/subscribe [:get (conj node-flag-db-path path)]))

  (fn [flag [_ path]]
    (node-open? flag path)))

(rf/reg-sub ::active-node-path
  ;; TODO: subscription here is a bug
  (fn [_ _]
    [(rf/subscribe [:get active-node-path])
     (rf/subscribe [:get @(rf/subscribe [:get active-node-path])])
     (rf/subscribe [:get node-selected-default-path])])

  (fn [[selected-node-path data default] [_ _path]]
    (if (and selected-node-path
             data)
      selected-node-path
      default)))

(defn- open-node [db path]
  (let [path (vec path)]
    (update-in db node-flag-db-path
               merge (into {}
                           (map (fn [idx]
                                  [(subvec path 0 (inc idx)) true]))
                           (range (count path))))))

(defn- close-node [db path]
  (update-in
   db node-flag-db-path
   (fn [flags]
     (into {}
           (map (fn [[other-path v]]
                  (if (= (take (count path) other-path)
                         path)
                    [other-path false]
                    [other-path v])))
           (assoc flags path false)))))

(macros/reg-event-db ::toggle-node
  (fn [db [_ path]]
    (if (node-open?
         (get-in db (conj node-flag-db-path path))
         path)
      (close-node db path)
      (open-node db path))))

(macros/reg-event-fx ::select-node
  (fn [{:keys [db]} [_ path open?]]
    (let [raw-type (get-in db (conj path :type))
          component-type (component/effective-type raw-type)]
      (cond->
        {:db (-> db
                 (assoc-in active-node-path path)
                 (open-node (cond-> path
                              (not open?) drop-last)))}
        (= component-type
           :heraldicon.entity.collection/element) (assoc :dispatch [::collection.element/highlight path])))))

(macros/reg-event-db ::node-select-default
  (fn [db [_ path valid-prefixes]]
    (let [current-selected-node (get-in db active-node-path)
          valid-path? (some (fn [path]
                              (= (take (count path) current-selected-node)
                                 path))
                            valid-prefixes)]
      (-> db
          (assoc-in node-selected-default-path path)
          (cond->
            (not valid-path?) (assoc-in active-node-path nil))))))

(defn- adjust-component-path-after-order-change [path elements-path index new-index]
  (let [elements-path-size (count elements-path)
        path-base (when (-> path count (>= elements-path-size))
                    (subvec path 0 elements-path-size))
        path-rest (when (-> path count (>= elements-path-size))
                    (subvec path (count elements-path)))
        current-index (first path-rest)
        path-rest (when (-> path-rest count (> 1))
                    (subvec path-rest 1))]
    (if (or (not= path-base elements-path)
            (= index new-index))
      path
      (if (nil? new-index)
        (cond
          (= current-index index) nil
          (> current-index index) (vec (concat path-base
                                               [(dec current-index)]
                                               path-rest))
          :else path)
        (cond
          (= current-index index) (vec (concat path-base
                                               [new-index]
                                               path-rest))
          (<= index current-index new-index) (vec (concat path-base
                                                          [(dec current-index)]
                                                          path-rest))
          (<= new-index current-index index) (vec (concat path-base
                                                          [(inc current-index)]
                                                          path-rest))
          :else path)))))

(defn change-selected-component-if-removed [db fallback-path]
  (if (get-in db (get-in db active-node-path))
    db
    (assoc-in db active-node-path fallback-path)))

(defn element-order-changed [db elements-path index new-index]
  (-> db
      (update-in active-node-path
                 adjust-component-path-after-order-change elements-path index new-index)
      (update-in collection.element/highlighted-element-path
                 adjust-component-path-after-order-change elements-path index new-index)
      (update-in submenu/open?-path
                 (fn [flags]
                   (into {}
                         (comp (filter first)
                               (map (fn [[k v]]
                                      [(adjust-component-path-after-order-change
                                        k elements-path index new-index) v])))
                         flags)))
      (update-in node-flag-db-path (fn [flags]
                                     (into {}
                                           (keep (fn [[path flag]]
                                                   (let [new-path (adjust-component-path-after-order-change
                                                                   path elements-path index new-index)]
                                                     (when new-path
                                                       [new-path flag]))))
                                           flags)))))
