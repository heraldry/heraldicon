(ns heraldicon.frontend.element.charge-type-select
  (:require
   [cljs.core.async]
   [clojure.string :as str]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.context :as c]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.debounce :as debounce]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.repository.charge-types :as repository.charge-types]
   [heraldicon.frontend.search-string :as search-string]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.heraldry.facets :as facets]
   [heraldicon.interface :as interface]
   [heraldicon.util.sanitize :as sanitize]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def charge-types-path
  (conj repository.charge-types/db-path-charge-types :data))

(def standard-shapes-path
  repository.charge-types/db-path-standard-shapes)

(def base-context
  (c/<< context/default :path charge-types-path))

(def search-db-path
  [::search])

(defn- remove-charge-count
  "Remove the last word of ' (<number>)' from the title if there is any."
  [title]
  (let [title (str/trim title)
        [title _count] (str/split title #" \(\d+\)$" 2)]
    title))

(rf/reg-sub ::search-title-matching-any
  :<- [:get search-db-path]

  (fn [search-string [_ title]]
    (let [title (search-string/normalize-string-for-match (or title ""))
          title (remove-charge-count title)
          words (search-string/split (or search-string ""))]
      (if (empty? words)
        true
        (reduce (fn [_ word]
                  (when (search-string/matches-word? title word)
                    (reduced true)))
                nil
                words)))))

;; TODO: it's not efficient to do a subscription for every node here
(defn- search-matching-any
  [title]
  @(rf/subscribe [::search-title-matching-any title]))

(rf/reg-sub ::search-title-matching-all
  :<- [:get search-db-path]

  (fn [search-string [_ title]]
    (let [title (search-string/normalize-string-for-match (or title ""))
          title (remove-charge-count title)
          words (search-string/split (or search-string ""))]
      (if (empty? words)
        true
        (every? (fn [word]
                  (search-string/matches-word? title word))
                words)))))

;; TODO: it's not efficient to do a subscription for every node here
(defn- search-matching-all
  [title]
  @(rf/subscribe [::search-title-matching-all title]))

(defn- force-open?
  []
  (-> @(rf/subscribe [:get search-db-path])
      count
      pos?))

(rf/reg-event-fx ::update-search-field
  (fn [{:keys [db]} [_ value]]
    {:db (assoc db ::add-error nil)
     ::debounce/dispatch [::debounce-update-search-field [:set search-db-path value] 250]}))

(defn- search-field
  []
  (let [tmp-value (r/atom @(rf/subscribe [:get search-db-path]))]
    (fn []
      [:input {:type "search"
               :placeholder "Search"
               :value @tmp-value
               :style {:width "20em"}
               :on-change #(do
                             (reset! tmp-value (-> % .-target .-value))
                             (rf/dispatch [::update-search-field @tmp-value]))}])))

(defn- any-node-matches?
  [node search-fn]
  (or (search-fn (:name node))
      (some #(any-node-matches? % search-fn) (:types node))))

(rf/reg-sub ::any-search-match?
  :<- [:get charge-types-path]

  (fn [charge-types _]
    (fn [search-fn]
      (some #(any-node-matches? % search-fn) (:types charge-types)))))

(rf/reg-event-fx ::add-charge-type
  (fn [{:keys [db]} [_ name context]]
    (let [session (session/data-from-db db)]
      {::add-charge-type-api [name session context]})))

(rf/reg-fx ::add-charge-type-api
  (fn [[name session context]]
    (cljs.core.async/go
      (try
        (<? (api/call :add-charge-type {:name name} session))
        (rf/dispatch [::repository.charge-types/clear])
        (rf/dispatch [::set-add-error nil])
        (rf/dispatch [::select-charge-type-by-name name context])
        (catch :default e
          (let [message (-> e ex-data :message)]
            (rf/dispatch [::set-add-error (or message (ex-message e))])))))))

(rf/reg-event-db ::select-charge-type-by-name
  (fn [db [_ name context]]
    (-> db
        (assoc-in (:path context) name)
        (assoc-in (conj submenu/open?-path (:path context)) false))))

(rf/reg-event-db ::set-add-error
  (fn [db [_ error]]
    (assoc db ::add-error error)))

(rf/reg-sub ::add-error
  (fn [db _]
    (get db ::add-error)))

(defn- search-bar
  []
  [:div {:style {:margin-bottom "10px"}}
   [search-field]
   [:a {:style {:margin-left "10px"}
        :on-click #(do
                     (rf/dispatch [::repository.charge-types/clear])
                     (.stopPropagation %))}
    [:i.fas.fa-sync-alt]]])

(rf/reg-event-db ::set-charge-type
  (fn [db [_ context path]]
    (let [attribute-path (:path context)
          {:keys [name types]} (get-in db path)
          leaf? (empty? types)]
      (cond-> (assoc-in db attribute-path name)
        leaf? (assoc-in (conj submenu/open?-path attribute-path) false)))))

(rf/reg-sub ::top-level-charge-type-paths
  :<- [:get (conj charge-types-path :types)]

  (fn [charge-types _]
    (->> charge-types
         (map-indexed vector)
         (sort-by #(some-> % second :name str/lower-case))
         (map (fn [[idx _]]
                (conj charge-types-path :types idx)))
         (into []))))

(rf/reg-sub ::augmented-top-level-charge-type-paths
  ;; Like ::top-level-charge-type-paths but also includes paths to any
  ;; standard (inline) shapes that aren't already represented in the DB tree.
  ;; Used by the arms autocomplete; the charges-list filter sticks with the
  ;; un-augmented sub so picking a synthetic node there can't end up
  ;; "filtering" against a charge_type that doesn't exist in the DB.
  :<- [:get (conj charge-types-path :types)]
  :<- [:get standard-shapes-path]

  (fn [[charge-types standard-shapes] _]
    (let [db-entries (map-indexed (fn [idx node]
                                    [node (conj charge-types-path :types idx)])
                                  charge-types)
          std-entries (map-indexed (fn [idx node]
                                     [node (conj standard-shapes-path idx)])
                                   standard-shapes)]
      (->> (concat db-entries std-entries)
           (sort-by #(some-> % first :name str/lower-case))
           (mapv second)))))

(defn- get-charge-type-path
  [charge-types charge-type]
  (let [charge-type (str/lower-case charge-type)
        path-nodes (tree-seq
                    #(contains? (second %) :types)
                    (fn [[path node]]
                      (map-indexed
                       (fn [idx child]
                         [(conj path :types idx) child])
                       (:types node)))
                    [[] charge-types])
        found-path (->> path-nodes
                        (filter (fn [[_ node]]
                                  (and (map? node)
                                       (some-> node :name clojure.string/lower-case (= charge-type)))))
                        first
                        first)]
    (when found-path
      (into charge-types-path found-path))))

(rf/reg-event-db ::set-active-node
  (fn [db [_ charge-types context]]
    (let [charge-type (get-in db (:path context))]
      (cond-> db
        charge-type (tree/select-node ::identifier (get-charge-type-path charge-types charge-type) true)))))

(defn- get-charge-type-path-by-slug
  "Walks the charge-types tree (in app-db) and returns the re-frame path of
   the node whose slugified name matches `slug`. Returns nil if no match."
  [charge-types slug]
  (let [path-nodes (tree-seq
                    #(contains? (second %) :types)
                    (fn [[path node]]
                      (map-indexed
                       (fn [idx child]
                         [(conj path :types idx) child])
                       (:types node)))
                    [[] charge-types])
        found-path (->> path-nodes
                        (filter (fn [[_ node]]
                                  (and (map? node)
                                       (some-> node :name facets/slugify-name (= slug)))))
                        first
                        first)]
    (when found-path
      (into charge-types-path found-path))))

(defn- standard-shape-path-by-slug
  "Returns the re-frame path of the synthetic standard-shape node whose
   slugified name matches `slug`, or nil."
  [standard-shapes slug]
  (some (fn [[idx node]]
          (when (= slug (facets/slugify-name (:name node)))
            (conj standard-shapes-path idx)))
        (map-indexed vector standard-shapes)))

(rf/reg-event-db ::filter-select-by-slug
  (fn [db [_ slug]]
    (let [charge-types (get-in db charge-types-path)
          standard-shapes (get-in db standard-shapes-path)
          path (when (seq slug)
                 (or (get-charge-type-path-by-slug charge-types slug)
                     (standard-shape-path-by-slug standard-shapes slug)))]
      (cond-> db
        path (tree/select-node ::filter-identifier path true)))))

(defn first-filter-match
  "DFS through the loaded charge_types tree using the same matcher the tree
   itself uses, returning the :name of the first node that matches the given
   search string. Returns nil if no match.

   Sibling order mirrors what the tree component displays:
   - Top-level types are sorted by lowercased name (see ::top-level-charge-type-paths).
   - Sub-types are sorted by [has-children?, lowercased name] (see
     heraldicon.frontend.component.charge-type/sort-key).
   So the first match returned is the one visually at the top of the filter.

   When the typed value slugifies to the same string as one of the matching
   node names, that node is preferred over the visually-first substring
   match (so `charge:lion` picks `Lion`, not `Lion's head`).

   `extra-top-level-nodes` lets the caller include synthetic top-level nodes
   (the missing standard shapes) so they participate in the alphabetical
   top-level sort just like DB-loaded ones."
  ([charge-types search-string]
   (first-filter-match charge-types nil search-string))
  ([charge-types extra-top-level-nodes search-string]
   (let [target-slug (facets/slugify-name (or search-string ""))
         words (search-string/split (or search-string ""))
         sort-top (fn [nodes]
                    (sort-by #(some-> % :name str/lower-case) nodes))
         sort-sub (fn [nodes]
                    (sort-by (juxt #(if (seq (:types %)) 0 1)
                                   #(some-> % :name str/lower-case))
                             nodes))]
     (when (seq words)
       (letfn [(substring-match? [node]
                 (let [title (-> node :name (or "") remove-charge-count
                                 search-string/normalize-string-for-match)]
                   (every? (fn [w] (search-string/matches-word? title w)) words)))
               (exact-match? [node]
                 (and (seq target-slug)
                      (= target-slug (some-> node :name facets/slugify-name))))
               (walk [pred node]
                 (cond
                   (not (map? node)) nil
                   (pred node) (:name node)
                   :else (some (partial walk pred) (sort-sub (:types node)))))]
         (let [top (sort-top (concat (:types charge-types)
                                     extra-top-level-nodes))]
           (or (some (partial walk exact-match?) top)
               (some (partial walk substring-match?) top))))))))

(defn- charge-type-select
  [context]
  (let [{:keys [status error]} @(rf/subscribe [::repository.charge-types/data #(rf/dispatch [::set-active-node % context])])
        editing? (= (:ns context) :heraldicon.frontend.charge-types/ns)]
    (case status
      :loading [status/loading]
      :error [status/error-display error]
      (let [search-fn (if editing?
                        search-matching-any
                        search-matching-all)
            any-match-fn @(rf/subscribe [::any-search-match?])
            search-string @(rf/subscribe [:get search-db-path])
            has-search? (seq search-string)
            no-results? (and has-search?
                             (not (any-match-fn search-fn)))
            logged-in? (some? (:session-id @(rf/subscribe [::session/data])))]
        [:div
         [search-bar]
         [tree/tree
          ::identifier
          @(rf/subscribe [::top-level-charge-type-paths])
          base-context
          :select-fn #(rf/dispatch [::set-charge-type context %])
          :search-fn search-fn
          :force-open? (force-open?)]
         (let [normalized (when has-search?
                            (sanitize/normalize-charge-type-name search-string))
               add-error @(rf/subscribe [::add-error])]
           [:<>
            (when (and no-results? logged-in? (seq normalized))
              [:div {:style {:margin-top "10px"
                             :display "flex"
                             :align-items "center"
                             :gap "5px"}}
               [:a {:style {:cursor "pointer"}
                    :on-click #(do
                                 (rf/dispatch [::add-charge-type normalized context])
                                 (.stopPropagation %))}
                [:i.fas.fa-plus]
                (str " " (tr :string.button/add) " \"" normalized "\"")]])
            (when add-error
              [:div.error-message {:style {:margin-top "10px"
                                           :max-width "80%"}}
               add-error])])]))))

(defmethod element/element :ui.element/charge-type-select [context]
  (when-let [option (interface/get-options context)]
    (let [current-value (interface/get-raw-data context)
          value (or current-value
                    :string.miscellaneous/none)
          {:ui/keys [label]} option]
      [:div.ui-setting {:data-tour "charge-type-select"}
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-charge-type
         [:div
          [tr value]]
         {:style {:position "fixed"
                  :transform "none"}}
         [:div {:style {:width "25em"
                        :height "25em"
                        :overflow-y "auto"}}
          [charge-type-select context]]]]])))

(defn charge-type-filter-tree
  ([on-select] (charge-type-filter-tree on-select nil))
  ([on-select {:keys [hide-search-bar? with-standard-shapes?]}]
   (let [{:keys [status error]} @(rf/subscribe [::repository.charge-types/data nil])
         paths-sub (if with-standard-shapes?
                     ::augmented-top-level-charge-type-paths
                     ::top-level-charge-type-paths)]
     (case status
       :loading [status/loading]
       :error [status/error-display error]
       [:div
        (when-not hide-search-bar?
          [search-bar])
        [tree/tree
         ::filter-identifier
         @(rf/subscribe [paths-sub])
         base-context
         :select-fn (fn [path]
                      (let [{:keys [name types]} @(rf/subscribe [:get path])]
                        (on-select name (empty? types))))
         :search-fn search-matching-all
         :force-open? (force-open?)]]))))
