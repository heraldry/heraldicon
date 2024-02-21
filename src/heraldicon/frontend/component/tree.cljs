(ns heraldicon.frontend.component.tree
  (:require
   [clojure.string :as str]
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
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private active-node-path
  [:ui :component-tree :selected-node])

(def ^:private highlight-node-path
  [:ui :component-tree :highlighted-node])

(def ^:private node-selected-default-path
  [:ui :component-tree :selected-node-default])

(def ^:private node-flag-db-path
  [:ui :component-tree :nodes])

(def ^:private edit-node-path
  [:ui :component-tree :edit-node])

(def ^:private dragged-node-path
  [:ui :component-tree :dragged-node])

(def ^:private dragged-over-node-path
  [:ui :component-tree :dragged-over-node])

(defn node-data [{:keys [path] :as context}]
  (merge {:open? @(rf/subscribe [::node-open? path])
          :highlighted? @(rf/subscribe [::node-highlighted? path])
          :selected? (= path @(rf/subscribe [::active-node-path]))
          :selectable? true}
         (frontend.component/node context)))

(defn- node-name-input
  [value-path]
  (let [ref (atom nil)
        value (r/atom @(rf/subscribe [:get value-path]))]
    (r/create-class
     {:component-did-mount (fn []
                             (doto @ref
                               .focus
                               .select
                               .scrollIntoViewIfNeeded))

      :reagent-render (fn [value-path]
                        [:input.node-name-input
                         {:ref #(reset! ref %)
                          :value @value
                          :on-click (fn [event]
                                      (.stopPropagation event))
                          :on-blur #(rf/dispatch [::complete-editing value-path @value])
                          :on-change #(reset! value (-> % .-target .-value))
                          :on-key-down (fn [event]
                                         (let [code (.-code event)]
                                           (case code
                                             "Enter" (rf/dispatch [::complete-editing value-path @value])
                                             "Escape" (rf/dispatch [::set-edit-node nil])
                                             nil)))}])})))

(defn- calculate-drop-area
  [event drop-options]
  (when (seq drop-options)
    (let [y (.-clientY event)
          target (.-target event)
          rect (.getBoundingClientRect target)
          ty (.-top rect)
          th (.-height rect)
          dy (- y ty)
          [above-cutoff inside-cutoff] (case drop-options
                                         #{:above} [1.1 1.1]
                                         #{:inside} [0 1.1]
                                         #{:below} [0 0]
                                         #{:above :below} [0.5 0.5]
                                         #{:above :inside} [0.25 1.1]
                                         #{:inside :below} [0 0.75]
                                         [0.25 0.75])]
      (when (<= 0 dy th)
        (cond
          (< dy (* above-cutoff th)) :above
          (< dy (* inside-cutoff th)) :inside
          :else :below)))))

(rf/reg-sub ::dragged?
  :<- [:get dragged-node-path]

  (fn [dragged-context [_ {:keys [path]}]]
    (= path (:path dragged-context))))

(rf/reg-sub ::drop-info
  :<- [:get dragged-over-node-path]

  (fn [dragged-over-data [_ {:keys [path]}]]
    (when (= path (:path (:context dragged-over-data)))
      dragged-over-data)))

(defn- drop-location
  [event drop-options-fn dragged-node-path dragged-over-node-path open?]
  (when drop-options-fn
    (let [drop-options (drop-options-fn dragged-node-path dragged-over-node-path open?)]
      (calculate-drop-area event drop-options))))

(defn node [{:keys [path] :as context} & {:keys [title
                                                 parent-buttons
                                                 force-open?
                                                 search-fn
                                                 extra]}]
  (let [{node-title :title
         :keys [open?
                highlighted?
                selected?
                selectable?
                nodes
                buttons
                annotation
                validation
                icon
                editable-path
                draggable?
                drop-options-fn
                drop-fn]} (node-data context)
        open? (or force-open? open?)
        openable? (-> nodes count pos?)
        edit-node @(rf/subscribe [::edit-node])
        title (or node-title title)
        buttons (concat buttons parent-buttons)
        {dragged-over-node :context
         where :where} @(rf/subscribe [::drop-info context])
        dragged-node-context @(rf/subscribe [:get dragged-node-path])
        dragged? (= path (:path dragged-node-context))
        dragged-over? dragged-over-node
        hide? (when search-fn
                (not (search-fn title)))]
    [:<>
     (when-not hide?
       [:div.node-name.clickable.no-select
        {:class [(when selected?
                   "selected")
                 (when-not selectable?
                   "unselectable")
                 (when highlighted?
                   "node-highlighted")
                 (when dragged-over?
                   (case where
                     :above "node-dragged-over-above"
                     :inside "node-dragged-over-inside"
                     :below "node-dragged-over-below"))
                 (when dragged?
                   "node-dragged")]
         :draggable draggable?
         :on-drag-over (fn [event]
                         (when-let [where (drop-location event drop-options-fn
                                                         (:path dragged-node-context)
                                                         path
                                                         (and openable?
                                                              open?))]
                           (.preventDefault event)
                           (rf/dispatch [:set dragged-over-node-path {:context context
                                                                      :where where}])))
         :on-drag-leave (fn [_event]
                          (rf/dispatch [:set dragged-over-node-path nil]))
         :on-drag-start (fn [_event]
                          (rf/dispatch [:set dragged-node-path context]))
         :on-drag-end (fn [_event]
                        (rf/dispatch [:set dragged-node-path nil])
                        (rf/dispatch [:set dragged-over-node-path nil]))
         :on-drop (fn [event]
                    (when-let [where (drop-location event
                                                    drop-options-fn
                                                    (:path dragged-node-context)
                                                    path
                                                    (and openable?
                                                         open?))]
                      (when drop-fn
                        (drop-fn dragged-node-context context where))))

         :on-click #(do
                      (when (or (not open?)
                                (not selectable?)
                                selected?)
                        (rf/dispatch [::toggle-node path]))
                      (when selectable?
                        (rf/dispatch [::select-node path]))
                      (.stopPropagation %))}

        (when (and dragged-over?
                   (#{:above :below} where))
          [:div {:style {:height "0px"
                         :border-top "2px solid #000"
                         :width "10em"
                         :position "absolute"
                         (if (= where :above)
                           :top
                           :bottom) -1
                         :pointer-events "none"}}])

        (when draggable?
          [:div.node-drag-handle
           [:i.ui-icon.fas.fa-grip-lines]])

        (if openable?
          [:span.node-icon.clickable
           {:on-click (js-event/handled #(rf/dispatch [::toggle-node path]))}
           [:i.fa.ui-icon {:class (if open?
                                    "fa-angle-down"
                                    "fa-angle-right")}]]
          [:span.node-icon])

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

        (if (and (= extra :second)
                 (some-> editable-path (= (:path edit-node))))
          [node-name-input editable-path]
          [tr title])

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
              buttons)])

     (when open?
       (into [:ul]
             (map (fn [{:keys [context title buttons]}]
                    ^{:key context}
                    [:li [node context
                          :title title
                          :parent-buttons buttons
                          :force-open? force-open?
                          :search-fn search-fn
                          :extra extra]]))
             nodes))]))

(defn tree [paths context & {:keys [search-fn force-open? extra]}]
  [:div.ui-tree
   (into [:ul]
         (map-indexed (fn [idx node-path]
                        ^{:key idx}
                        [:li
                         (if (= node-path :spacer)
                           [:div {:style {:height "1em"}}]
                           [node (c/<< context :path node-path)
                            :force-open? force-open?
                            :search-fn search-fn
                            :extra extra])]))
         paths)])

(defn- node-open-by-default? [path]
  (or (#{[:coat-of-arms]
         [:helms]
         [:ornaments]
         [:elements]
         [:charge-types]}
       (take-last 1 path))
      (#{[:coat-of-arms :field]
         [:forms :heraldicon.frontend.charge-types/form]}
       (take-last 2 path))
      (#{[:forms :heraldicon.frontend.charge-types/form :types]}
       (butlast (take-last 4 path)))
      (#{[:coat-of-arms :field :components]}
       (butlast (take-last 4 path)))
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

(rf/reg-sub ::node-highlighted?
  (fn [[_ path] _]
    [(rf/subscribe [:get (conj highlight-node-path path)])
     (rf/subscribe [:get (conj highlight-node-path (drop-last path))])
     (rf/subscribe [:get (conj highlight-node-path (conj path :field))])])

  (fn [[highlighted?
        parent-component-highlighted?
        child-field-highlighted?] [_ path]]
    (or highlighted?
        (and (= (last path) :field)
             parent-component-highlighted?)
        child-field-highlighted?)))

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

(defn- determine-component-path [db path]
  (let [path (if (get-in db (conj path :field))
               (conj path :field)
               path)
        ;; if this is the field of a subfield, then use the path of the subfield,
        ;; because that's the node displayed in the tree
        path (if (= (first (take-last 3 path)) :fields)
               (vec (drop-last path))
               path)]
    path))

(macros/reg-event-fx ::select-node-from-preview
  (fn [{:keys [db]} [_ path open?]]
    (let [path (determine-component-path db path)]
      {:db db
       :dispatch [::select-node path open?]})))

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

(macros/reg-event-db ::set-edit-node
  (fn [db [_ context]]
    (assoc-in db edit-node-path context)))

(macros/reg-event-db ::complete-editing
  (fn [db [_ editable-path value]]
    (let [real-value (-> value
                         str/trim
                         (str/replace #"\s+" " "))]
      (cond-> (assoc-in db edit-node-path nil)
        (not (str/blank? real-value)) (assoc-in editable-path real-value)))))

(rf/reg-sub ::edit-node
  :<- [:get edit-node-path]

  (fn [edit-node _]
    edit-node))

(macros/reg-event-fx ::highlight-node
  (fn [{:keys [db]} [_ path]]
    (let [path (determine-component-path db path)]
      {:db (assoc-in db (conj highlight-node-path path) true)})))

(macros/reg-event-fx ::unhighlight-node
  (fn [{:keys [db]} [_ path]]
    (let [path (determine-component-path db path)]
      {:db (update-in db highlight-node-path dissoc path)})))

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
