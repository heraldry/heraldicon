(ns heraldicon.frontend.component.tree
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as frontend.component]
   [heraldicon.frontend.element.hover-menu :as hover-menu]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]
   [reagent.core :as r])
  (:require-macros [reagent.ratom :refer [reaction]]))

(defn- active-node-path
  [identifier]
  [:ui :component-tree identifier :selected-node])

(defn- highlight-node-path
  [identifier]
  [:ui :component-tree identifier :highlighted-node])

(def ^:private node-selected-default-path
  [:ui :component-tree :selected-node-default])

(def ^:private node-flag-db-path
  [:ui :component-tree :nodes])

(def ^:private edit-node-path
  [:ui :component-tree :edit-node])

(def ^:private dragged-over-node-path
  [:ui :component-tree :drop-node])

(def ^:private drag-node-ref
  (atom nil))

(def highlighted-collection-element-path
  [:ui :collection-library :selected-element])

(defn node-data [{:keys [path]
                  ::keys [identifier]
                  :as context}]
  (merge {:open? @(rf/subscribe [::node-open? path])
          :highlighted? @(rf/subscribe [::node-highlighted? identifier path])
          :selected? @(rf/subscribe [::node-active? identifier path])
          :selectable? true}
         (frontend.component/node context)))

(defn- node-name-input
  [_identifier value-path]
  (let [ref (atom nil)
        value (r/atom @(rf/subscribe [:get value-path]))]
    (r/create-class
     {:component-did-mount (fn []
                             (doto @ref
                               .focus
                               .select
                               .scrollIntoViewIfNeeded))

      :reagent-render (fn [identifier value-path]
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
                                             "Escape" (rf/dispatch [::set-edit-node identifier nil])
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

(rf/reg-sub ::dragged-over-location
  :<- [:get dragged-over-node-path]

  (fn [dragged-over-node [_ path]]
    (when (= path (:path (:context dragged-over-node)))
      (:where dragged-over-node))))

(defn- drop-location
  [event drop-options-fn drag-node drop-node]
  (when drop-options-fn
    (let [drop-options (drop-options-fn drag-node drop-node)]
      (calculate-drop-area event drop-options))))

(defn node [{:keys [path]
             ::keys [identifier]
             :as context}
            & {:keys [title
                      parent-buttons
                      force-open?
                      search-fn
                      filter-fn
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
                drop-fn
                select-fn]} (node-data context)
        open? (or force-open? open?)
        openable? (-> nodes count pos?)
        editing-node? @(rf/subscribe [::editing-node? editable-path])
        title (or node-title title)
        buttons (concat buttons parent-buttons)
        dragged-over-location @(rf/subscribe [::dragged-over-location path])
        hide? (or (and filter-fn
                       (not (filter-fn path)))
                  (and search-fn
                       (not (search-fn title))))
        node-type (interface/get-raw-data (c/++ context :type))
        drag-info (when (or draggable?
                            drop-fn)
                    (let [parent-context (interface/parent-node-context context)
                          parent-type (interface/get-raw-data (c/++ parent-context :type))]
                      {:context context
                       :type node-type
                       :parent-context parent-context
                       :parent-type parent-type
                       :open? (and openable?
                                   open?)}))]
    [:<>
     (when-not hide?
       [:div.node-name.clickable.no-select
        {:class [(when selected?
                   "selected")
                 (when-not selectable?
                   "unselectable")
                 (when highlighted?
                   "node-highlighted")
                 (when (= dragged-over-location :inside)
                   "node-drop-inside")]
         :style {:cursor (when draggable?
                           "grab")}
         :draggable draggable?
         :on-drag-over (when drop-fn
                         (fn [event]
                           (when-let [where (drop-location event drop-options-fn
                                                           @drag-node-ref
                                                           drag-info)]
                             (.preventDefault event)
                             (rf/dispatch [:set dragged-over-node-path (assoc drag-info :where where)]))))
         :on-drag-leave (when drop-fn
                          (fn [_event]
                            (rf/dispatch [:set dragged-over-node-path nil])))
         :on-drag-start (when draggable?
                          (fn [_event]
                            (reset! drag-node-ref drag-info)))
         :on-drag-end (when draggable?
                        (fn [_event]
                          (reset! drag-node-ref nil)
                          (rf/dispatch [:set dragged-over-node-path nil])))
         :on-drop (when drop-fn
                    (fn [event]
                      (when-let [where (drop-location event
                                                      drop-options-fn
                                                      @drag-node-ref
                                                      drag-info)]
                        (drop-fn @drag-node-ref (assoc drag-info :where where)))))

         :on-click (or select-fn
                       #(do
                          (when (or (not open?)
                                    (not selectable?)
                                    selected?)
                            (rf/dispatch [::toggle-node path]))
                          (when selectable?
                            (rf/dispatch [::select-node identifier path]))
                          (.stopPropagation %)))}

        (when (#{:above :below} dragged-over-location)
          [:div.node-drop-insert {:style {(if (= dragged-over-location :above)
                                            :top
                                            :bottom) -1}}])

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
                 editing-node?)
          [node-name-input identifier editable-path]
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
                                                         :cursor (if disabled?
                                                                   "not-allowed"
                                                                   "pointer")}}]
                                    :disabled? disabled?
                                    :require-click? true])))
              buttons)

        (into [:span {:style {:margin-left "0.5em"}}]
              (comp (remove :menu)
                    (map-indexed (fn [idx {:keys [icon handler disabled? title remove? margin]}]
                                   [:span.node-icon
                                    {:class (when disabled? "disabled")
                                     :title (tr title)
                                     :style {:margin-left (if (and (pos? idx)
                                                                   remove?)
                                                            "0.5em"
                                                            margin)
                                             :cursor (if disabled?
                                                       "not-allowed"
                                                       "pointer")}}
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
                    [:li [node
                          context
                          :title title
                          :parent-buttons buttons
                          :force-open? force-open?
                          :search-fn search-fn
                          :filter-fn filter-fn
                          :extra extra]]))
             nodes))]))

(defn tree [identifier paths context & {:keys [search-fn filter-fn force-open? extra]}]
  [:div.ui-tree
   (into [:ul]
         (map-indexed (fn [idx node-path]
                        ^{:key idx}
                        [:li
                         (if (= node-path :spacer)
                           [:div {:style {:height "1em"}}]
                           [node
                            (-> context
                                (c/<< :path node-path)
                                (c/<< ::identifier identifier))
                            :force-open? force-open?
                            :search-fn search-fn
                            :filter-fn filter-fn
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
         [:heraldicon.entity.type/collection :data]}
       (take-last 2 path))
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
  (fn [[_ identifier path] _]
    (let [node-path (highlight-node-path identifier)]
      [(rf/subscribe [:get (conj node-path path)])
       (rf/subscribe [:get (conj node-path (drop-last path))])
       (rf/subscribe [:get (conj node-path (conj path :field))])]))

  (fn [[highlighted?
        parent-component-highlighted?
        child-field-highlighted?] [_ _identifier path]]
    (or highlighted?
        (and (= (last path) :field)
             parent-component-highlighted?)
        child-field-highlighted?)))

(rf/reg-sub-raw ::active-node-path
  (fn [_app-db [_ identifier]]
    (reaction
     (let [path (active-node-path identifier)
           selected-node-path @(rf/subscribe [:get path])
           data @(rf/subscribe [:get @(rf/subscribe [:get path])])
           default @(rf/subscribe [:get node-selected-default-path])]
       (if (and selected-node-path
                data)
         selected-node-path
         default)))))

(rf/reg-sub ::node-active?
  (fn [[_ identifier _path]]
    (rf/subscribe [::active-node-path identifier]))

  (fn [active-node-path [_ _ path]]
    (= path active-node-path)))

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
  (fn [{:keys [db]} [_ identifier path open?]]
    (let [path (determine-component-path db path)]
      {:db db
       :dispatch [::select-node identifier path open?]})))

(defn select-node
  [db identifier path open?]
  (-> db
      (assoc-in (active-node-path identifier) path)
      (open-node (vec (cond-> path
                        (not open?) drop-last)))))

(macros/reg-event-fx ::select-node
  (fn [{:keys [db]} [_ identifier path open?]]
    {:db (select-node db identifier path open?)}))

(defn set-edit-node
  [db identifier context]
  (-> db
      (assoc-in edit-node-path context)
      (select-node identifier (vec (drop-last (:path context))) false)))

(macros/reg-event-db ::set-edit-node
  (fn [db [_ identifier context]]
    (set-edit-node db identifier context)))

(macros/reg-event-db ::complete-editing
  (fn [db [_ editable-path value]]
    (let [real-value (-> value
                         str/trim
                         (str/replace #"\s+" " "))]
      (cond-> (assoc-in db edit-node-path nil)
        (not (str/blank? real-value)) (assoc-in editable-path real-value)))))

(rf/reg-sub ::editing-node?
  :<- [:get edit-node-path]

  (fn [edit-node [_ path]]
    (= (:path edit-node) path)))

(macros/reg-event-fx ::highlight-node
  (fn [{:keys [db]} [_ identifier path]]
    (let [path (determine-component-path db path)]
      {:db (assoc-in db (conj (highlight-node-path identifier) path) true)})))

(macros/reg-event-fx ::unhighlight-node
  (fn [{:keys [db]} [_ identifier path]]
    (let [path (determine-component-path db path)]
      {:db (update-in db (highlight-node-path identifier) dissoc path)})))

(macros/reg-event-db ::node-select-default
  (fn [db [_ identifier path valid-prefixes]]
    (let [current-selected-node (get-in db (active-node-path identifier))
          valid-path? (some (fn [path]
                              (= (take (count path) current-selected-node)
                                 path))
                            valid-prefixes)]
      (-> db
          (assoc-in node-selected-default-path path)
          (cond->
            (not valid-path?) (assoc-in (active-node-path identifier) nil))))))

;; TODO: no identifier
(defn change-selected-component-if-removed [db fallback-path]
  db
  #_(if (get-in db (get-in db (active-node-path identifier)))
      db
      (assoc-in db (active-node-path identifier) fallback-path)))

(defn- adjust-component-path-after-element-removed [path elements-path index]
  (let [elements-path-size (count elements-path)
        path-base (when (-> path count (>= elements-path-size))
                    (subvec path 0 elements-path-size))
        path-rest (when (-> path count (>= elements-path-size))
                    (subvec path (count elements-path)))
        current-index (first path-rest)
        path-rest (when (-> path-rest count (> 1))
                    (subvec path-rest 1))]
    (if (not= path-base elements-path)
      path
      (cond
        (= current-index index) nil
        (> current-index index) (vec (concat path-base
                                             [(dec current-index)]
                                             path-rest))
        :else path))))

(defn element-removed [db identifier path]
  (let [elements-path (vec (drop-last path))
        index (last path)]
    (-> db
        (update-in (active-node-path identifier)
                   adjust-component-path-after-element-removed elements-path index)
        (update-in highlighted-collection-element-path
                   adjust-component-path-after-element-removed elements-path index)
        (update-in submenu/open?-path
                   (fn [flags]
                     (into {}
                           (comp (filter first)
                                 (map (fn [[k v]]
                                        [(adjust-component-path-after-element-removed
                                          k elements-path index) v])))
                           flags)))
        (update-in node-flag-db-path (fn [flags]
                                       (into {}
                                             (keep (fn [[path flag]]
                                                     (let [new-path (adjust-component-path-after-element-removed
                                                                     path elements-path index)]
                                                       (when new-path
                                                         [new-path flag]))))
                                             flags))))))

(defn- adjust-component-path-after-element-inserted [path elements-path index]
  (let [elements-path-size (count elements-path)
        path-base (when (-> path count (>= elements-path-size))
                    (subvec path 0 elements-path-size))
        path-rest (when (-> path count (>= elements-path-size))
                    (subvec path (count elements-path)))
        current-index (first path-rest)
        path-rest (when (-> path-rest count (> 1))
                    (subvec path-rest 1))]
    (if (not= path-base elements-path)
      path
      (cond
        (<= index current-index index) (vec (concat path-base
                                                    [(inc current-index)]
                                                    path-rest))
        :else path))))

(defn element-inserted [db identifier path]
  (let [elements-path (vec (drop-last path))
        index (last path)]
    (-> db
        (update-in (active-node-path identifier)
                   adjust-component-path-after-element-inserted elements-path index)
        (update-in highlighted-collection-element-path
                   adjust-component-path-after-element-inserted elements-path index)
        (update-in submenu/open?-path
                   (fn [flags]
                     (into {}
                           (comp (filter first)
                                 (map (fn [[k v]]
                                        [(adjust-component-path-after-element-inserted
                                          k elements-path index) v])))
                           flags)))
        (update-in node-flag-db-path (fn [flags]
                                       (into {}
                                             (keep (fn [[path flag]]
                                                     (let [new-path (adjust-component-path-after-element-inserted
                                                                     path elements-path index)]
                                                       (when new-path
                                                         [new-path flag]))))
                                             flags))))))
