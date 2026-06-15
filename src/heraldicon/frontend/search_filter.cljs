(ns heraldicon.frontend.search-filter
  (:require
   ["react-infinite-scroll-component" :as InfiniteScroll]
   [clojure.string :as str]
   [heraldicon.avatar :as avatar]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.entity.core :as entity]
   [heraldicon.frontend.element.charge-type-select :as charge-type-select]
   [heraldicon.frontend.element.select :as select]
   [heraldicon.frontend.element.tags :as tags]
   [heraldicon.frontend.entity.action.favorite :as favorite]
   [heraldicon.frontend.entity.preview :as preview]
   [heraldicon.frontend.facet-autocomplete :as facet-autocomplete]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.parameters :as parameters]
   [heraldicon.frontend.repository.charge-types :as repository.charge-types]
   [heraldicon.frontend.repository.entity-search :as entity-search]
   [heraldicon.frontend.repository.user :as repository.user]
   [heraldicon.frontend.search-string :as search-string]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.heraldry.facets :as facets]
   [heraldicon.localization.string :as string]
   [heraldicon.static :as static]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private default-ownership
  :all)

(def ^:private default-access
  :all)

(def ^:private default-sorting
  :favorites)

(def ^:private default-list-mode
  :normal)

(def ^:private default-favorites?
  false)

(def default-page-size
  20)

(defn- filter-temporary-search-string-path [id]
  [:ui :filter id :filter-temporary-search-string])

(defn- filter-search-string-path [id]
  [:ui :filter id :filter-search-string])

(defn- filter-list-mode-path [id]
  [:ui :filter id :filter-list-mode])

(defn- filter-favorites?-path [id]
  [:ui :filter id :filter-favorites?])

(defn- filter-ownership-path [id]
  [:ui :filter id :filter-ownership])

(defn- filter-access-path [id]
  [:ui :filter id :filter-access])

(defn- filter-sorting-path [id]
  [:ui :filter id :filter-sorting])

(defn- filter-tags-path [id]
  [:ui :filter id :filter-tags])

(defn- filter-charge-type-path [id]
  [:ui :filter id :filter-charge-type])

(defn- filter-charge-type-open?-path [id]
  [:ui :filter id :filter-charge-type-open?])

(macros/reg-event-db ::filter-toggle-tag
  (fn [db [_ id tag]]
    (update-in db (filter-tags-path id) (fn [current-tags]
                                          (if (get current-tags tag)
                                            (dissoc current-tags tag)
                                            (assoc current-tags tag true))))))

(rf/reg-sub ::search-result-item
  (fn [[_ id entity-type _item-id] _]
    (rf/subscribe [::entity-search/data-raw id entity-type]))

  (fn [items [_ _id _entity-type item-id]]
    (first (filter #(= (:id %) item-id) items))))

(defn- new-badge []
  [:img.new-badge {:src (static/static-url "/img/new-badge.png")}])

(defn- updated-badge []
  [:img.updated-badge {:src (static/static-url "/img/updated-badge.png")}])

(defn- get-list-mode [id options]
  (let [list-mode-path (filter-list-mode-path id)]
    (or @(rf/subscribe [:get list-mode-path])
        (:default-list-mode options)
        default-list-mode)))

(defn- get-access [id]
  (or @(rf/subscribe [:get {:path (filter-access-path id)}])
      default-access))

(defn- get-ownership [id {:keys [hide-ownership-filter?]}]
  (if-not hide-ownership-filter?
    @(rf/subscribe [:get (filter-ownership-path id)])
    default-ownership))

(defn- get-sorting [id {:keys [initial-sorting-mode]}]
  (or @(rf/subscribe [:get {:path (filter-sorting-path id)}])
      initial-sorting-mode
      default-sorting))

(defn- get-favorites? [id]
  (or @(rf/subscribe [:get (filter-favorites?-path id)])
      default-favorites?))

(defn- get-search-string [id]
  (or @(rf/subscribe [:get (filter-search-string-path id)])
      ""))

(defn- get-tags [id]
  (into []
        (keep
         (fn [[key value]]
           (when value
             key)))
        @(rf/subscribe [:get (filter-tags-path id)])))

(defn- get-charge-type [id]
  @(rf/subscribe [:get (filter-charge-type-path id)]))

(defn- result-card [id item-id kind on-select {:keys [selection-placeholder?
                                                      filter-tags
                                                      title-fn]
                                               :as options}]
  (let [{:keys [username]
         :as item} @(rf/subscribe [::search-result-item id kind item-id])
        selected? false
        own-username (:username @(rf/subscribe [::session/data]))
        small? (= (get-list-mode id options) :small)
        title-fn (or title-fn :name)
        title (title-fn item)]
    [:li.filter-result-card-wrapper
     [:div.filter-result-card {:class (when (and item selected?) "selected")
                               :style (when selection-placeholder?
                                        {:border "1px solid #888"
                                         :border-radius 0})}
      (when-not small?
        [:div.filter-result-card-header
         [:div.filter-result-card-owner
          (when item
            (let [{owner-data :user} @(rf/subscribe [::repository.user/data username])]
              [:a {:href (attribution/full-url-for-username username)
                   :target "_blank"
                   :title username}
               [:img {:src (avatar/url-from-user owner-data)
                      :style (avatar/shape-style (:uncropped-avatar? owner-data))}]]))]
         [:div.filter-result-card-title
          {:title title}
          title]
         (when item
           [:div.filter-result-card-access
            (when (= own-username username)
              (if (-> item :access (= :public))
                [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
                [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]]))])])
      [(if item
         :a.filter-result-card-preview
         :div.filter-result-card-preview) (merge {:title (when item
                                                           (tr (string/str-tr
                                                                title " " :string.miscellaneous/by " " username)))}
                                                 (when on-select
                                                   (on-select item)))
       (if item
         [preview/image kind item]
         [:div.filter-no-item-selected
          [tr :string.miscellaneous/no-item-selected]])
       (when-not small?
         (cond
           (entity/recently-created? item) [new-badge]
           (entity/recently-updated? item) [updated-badge]
           :else nil))]

      (when-not small?
        [:div.filter-result-card-tags
         (when item
           [:<>
            [:div.favorites {:style {:padding-left "10px"}}
             [favorite/button item-id :height 18]]
            [tags/tags-view (-> item :tags keys)
             :on-click #(rf/dispatch [::filter-toggle-tag id %])
             :selected filter-tags
             :style {:display "flex"
                     :flex-flow "row"
                     :flex-wrap "wrap"
                     :width "auto"
                     :overflow "hidden"
                     :height "25px"}]])])]]))

(defn- update-params [id options]
  (let [query (get-search-string id)
        ownership (get-ownership id options)
        access (get-access id)
        tags (get-tags id)
        favorites? (get-favorites? id)
        sort (get-sorting id options)
        list-mode (get-list-mode id options)
        charge-type (get-charge-type id)]
    (rf/dispatch [::parameters/set
                  (cond-> {}
                    (not (str/blank? query)) (assoc :q query)
                    (and ownership
                         (not= ownership default-ownership)) (assoc :ownership ownership)
                    (not= access default-access) (assoc :access access)
                    (not= list-mode default-list-mode) (assoc :mode list-mode)
                    (seq tags) (assoc :tags tags)
                    favorites? (assoc :favorites true)
                    (not= sort default-sorting) (assoc :sort sort)
                    (seq charge-type) (assoc :charge-type charge-type))])))

(defn- prepare-query [id {:keys [filter-username]
                          :as options}]
  (let [charge-type (get-charge-type id)
        {:keys [facets phrase-text]} (facets/split-input (get-search-string id))]
    (cond-> {:phrases (search-string/split phrase-text)
             :access (get-access id)
             :username (or (when (= (get-ownership id options) :mine)
                             (:username @(rf/subscribe [::session/data])))
                           filter-username)
             :tags (get-tags id)
             :favorites? (get-favorites? id)
             :sort (get-sorting id options)
             :page-size (or (get options :page-size)
                            default-page-size)}
      (seq facets) (assoc :facets facets)
      (seq charge-type) (assoc :charge-type charge-type))))

(defn- get-items-subscription [id kind options]
  (rf/subscribe [::entity-search/data id kind (prepare-query id options)]))

(macros/reg-event-db ::copy-search-string-to-query
  (fn [db [_ id]]
    (let [search-value (get-in db (filter-temporary-search-string-path id))]
      (assoc-in db (filter-search-string-path id) search-value))))

(defn- results-count [id kind options]
  (let [items-subscription (get-items-subscription id kind options)]
    [status/default
     items-subscription
     (fn [{:keys [total entities]}]
       [:div {:style {:display "inline"
                      :margin-left "10px"}}
        (count entities) " / "
        total
        " "
        [tr (if (= total 1)
              :string.miscellaneous/item
              :string.miscellaneous/items)]])
     :on-error (fn [_])
     :on-default (fn [_])]))

(defn- results [id kind on-select {:keys [page-size
                                          display-selected-item?
                                          selected-item]
                                   :as options}]
  (let [items-subscription (get-items-subscription id kind options)
        crawler? @(rf/subscribe [:get [:ui :crawler?]])
        crawler-next-list-page @(rf/subscribe [:get [:ui :crawler-next-list-page]])]
    (when (#{:arms-list
             :charge-list
             :ribbon-list
             :collection-list} id)
      (update-params id options))
    [status/default
     items-subscription
     (fn [{:keys [entities total tags]}]
       (let [filter-tags @(rf/subscribe [:get (filter-tags-path id)])
             small? (= (get-list-mode id options) :small)
             page-size (cond-> (or page-size default-page-size)
                         small? (* 5))
             results-id (str "filter-results-" id)]
         [:<>
          [:div.filter-component-tags
           [tags/tags-view tags
            :on-click #(rf/dispatch [::filter-toggle-tag id %])
            :selected filter-tags
            :style {:display "flex"
                    :flex-flow "row"
                    :flex-wrap "wrap"
                    :width "auto"
                    :overflow "hidden"
                    :height "25px"}]]

          [:div.filter-component-results {:id results-id}
           (if (empty? entities)
             [:div [tr :string.miscellaneous/none]]
             [:<>
              [:> InfiniteScroll
               {:dataLength (count entities)
                :hasMore (not= (count entities) total)
                :next #(rf/dispatch [::entity-search/load-more id kind page-size])
                :scrollableTarget results-id
                :style {:overflow "visible"}}
               [:ul.filter-results {:class (when small? "small")}
                (when display-selected-item?
                  [result-card id (:id selected-item) kind nil
                   (assoc options
                          :selection-placeholder? true)])
                (into [:<>]
                      (map (fn [item]
                             ^{:key (:id item)}
                             [result-card id (:id item) kind on-select options]))
                      entities)
                (when (< (count entities) total)
                  [:li.filter-result-card-wrapper.filter-component-show-more
                   [:button.button {:on-click #(rf/dispatch [::entity-search/load-more id kind page-size])}
                    [tr :string.miscellaneous/show-more]]])]]
              (when (and crawler?
                         crawler-next-list-page)
                [:a {:href crawler-next-list-page} "Next page"])])]]))]))

(defn- list-mode [id options]
  (let [current-list-mode (get-list-mode id options)]
    (into [:div {:style {:display "inline-block"
                         :margin-left "10px"}}]
          (map (fn [[list-mode class]]
                 ^{:key list-mode}
                 [:a {:style {:margin-left "10px"}
                      :href "#"
                      :on-click (js-event/handled #(rf/dispatch [:set (filter-list-mode-path id) list-mode]))}
                  [:i {:class class
                       :style {:color (when (not= current-list-mode list-mode)
                                        "#ccc")}}]]))
          [[:normal "fas fa-th-large"]
           [:small "fas fa-th"]])))

(def ^:private dropdown-base-style
  {:position "absolute"
   :top "100%"
   :left 0
   :z-index 50
   :background "white"
   :border "1px solid #ddd"
   :border-radius "0 0 4px 4px"
   :box-shadow "2px 4px 10px rgba(0,0,0,0.15)"})

(defn- search-input [id _kind _options]
  (let [path (filter-temporary-search-string-path id)
        value-sub (rf/subscribe [:get path])
        tmp-value (r/atom @value-sub)
        open? (r/atom false)
        cursor (r/atom (count (or @value-sub "")))
        input-ref (atom nil)
        container-ref (atom nil)
        last-applied-slug (atom ::unset)
        last-applied-tree-search (atom ::unset)
        selected-index (r/atom 0)
        ;; While we're restoring the cursor after a suggestion apply, focus
        ;; events fire and would otherwise clobber our cursor atom with the
        ;; browser's default selectionStart. This flag suppresses that path.
        applying? (atom false)
        track-cursor! (fn [e]
                        (when-not @applying?
                          (reset! cursor (-> e .-target .-selectionStart))))
        ;; Document-level outside-click detection. Beats a fullscreen overlay
        ;; because the overlay would either catch clicks inside the input
        ;; (closing the dropdown and blurring focus) or require z-index
        ;; gymnastics that don't actually fix the stacking-context issue.
        handle-outside-click (fn [e]
                               (let [target (.-target e)
                                     container @container-ref]
                                 (when (and container target
                                            (not (.contains container target)))
                                   (reset! open? false))))
        _ (add-watch open? ::outside-click
                     (fn [_ _ _ new-val]
                       (if new-val
                         (.addEventListener js/document "mousedown" handle-outside-click)
                         (.removeEventListener js/document "mousedown" handle-outside-click))))]
    ;; keep tmp-value in sync with subscription. Only nudge the cursor on
    ;; externally-driven changes (URL params, programmatic resets) — on-change
    ;; already moved it in lockstep with the user's typing, and this watch
    ;; also re-fires for our own dispatches.
    (add-watch value-sub ::sync
               (fn [_ _ _ new-val]
                 (when (not= new-val @tmp-value)
                   (reset! cursor (count (or new-val ""))))
                 (reset! tmp-value new-val)))
    (fn [id kind _options]
      (let [tree-key (when (= kind :arms)
                       (facet-autocomplete/tree-key @tmp-value @cursor))
            suggestions (when (and (= kind :arms) (not tree-key))
                          (facet-autocomplete/suggestions @tmp-value @cursor))
            ;; When the tree picker is showing, keep its active node in sync
            ;; with the value under the cursor. Plain atom (not r/atom) so
            ;; this doesn't trigger our own re-render; we only deref it for
            ;; the cheap equality check. Guard on charge-types being loaded
            ;; — on the very first open the data is still fetching, so the
            ;; lookup would silently fail; the deref also re-renders us when
            ;; the load completes so the dispatch fires then.
            _ (when (and tree-key @open?)
                (let [data-status (:status @(rf/subscribe [::repository.charge-types/data nil]))
                      slug (facet-autocomplete/current-value @tmp-value @cursor)]
                  (when (and (= :done data-status)
                             (not= slug @last-applied-slug))
                    (reset! last-applied-slug slug)
                    (rf/dispatch [::charge-type-select/filter-select-by-slug slug]))))
            ;; Mirror the token's value into the tree's built-in search field
            ;; so the user's typing in the outer input filters the tree.
            ;; Empty (or no-tree) state clears the filter so it doesn't leak
            ;; to other lists or persist across reopens.
            _ (let [desired (if (and tree-key @open?)
                              (or (facet-autocomplete/current-value @tmp-value @cursor) "")
                              "")]
                (when (not= desired @last-applied-tree-search)
                  (reset! last-applied-tree-search desired)
                  (rf/dispatch [::charge-type-select/update-search-field desired])))
            suggestion-count (count suggestions)
            effective-selected (if (pos? suggestion-count)
                                 (min (max 0 @selected-index) (dec suggestion-count))
                                 0)
            apply-suggestion! (fn [s]
                                (let [[new-val new-cursor]
                                      (facet-autocomplete/apply-suggestion @tmp-value @cursor s)]
                                  (reset! applying? true)
                                  (reset! tmp-value new-val)
                                  (reset! cursor new-cursor)
                                  (reset! selected-index 0)
                                  ;; Update the temp search string so re-renders
                                  ;; see the new value, but DON'T commit it to
                                  ;; the search-string path — the search only
                                  ;; fires when the user clicks the search
                                  ;; button (or hits Enter / blurs).
                                  (rf/dispatch-sync [:set path new-val])
                                  ;; A "complete" token (no trailing colon) is
                                  ;; the natural place to close the dropdown so
                                  ;; the next Enter fires the search. Suggestions
                                  ;; ending in ":" mean the user just picked a
                                  ;; key and now wants to see values — keep open.
                                  (when-not (str/ends-with? s ":")
                                    (reset! open? false))
                                  ;; The reset!s above queue a Reagent render
                                  ;; and the dispatch-sync queues a re-frame
                                  ;; render — both update the controlled input.
                                  ;; r/after-render only fires between them in
                                  ;; some orderings, so the second commit re-
                                  ;; shifts our cursor (by the inserted-text
                                  ;; length, since React's controlled-input
                                  ;; logic shifts based on length diff). Stack
                                  ;; a setTimeout inside after-render to land
                                  ;; the restore strictly after both have
                                  ;; settled.
                                  (r/after-render
                                   (fn []
                                     (js/setTimeout
                                      (fn []
                                        (when-let [node @input-ref]
                                          (.focus node)
                                          (.setSelectionRange node new-cursor new-cursor))
                                        (reset! applying? false))
                                      0)))))
            apply-tree-top! (fn []
                              (let [data (some-> @(rf/subscribe [::repository.charge-types/data nil]) :data)
                                    standard-shapes @(rf/subscribe [:get charge-type-select/standard-shapes-path])
                                    slug (facet-autocomplete/current-value @tmp-value @cursor)
                                    match (charge-type-select/first-filter-match data standard-shapes slug)]
                                (when match
                                  (apply-suggestion! (str tree-key ":" (facets/slugify-name match)))
                                  (reset! open? false)
                                  true)))]
        [:div.search-field {:class (when (= kind :arms) "wide")
                            :ref #(reset! container-ref %)
                            :style {:position "relative"}}
         [:i.fas.fa-search]
         [:input {:name "search"
                  :type "search"
                  :value @tmp-value
                  :autoComplete "off"
                  :ref #(reset! input-ref %)
                  :on-focus (fn [e]
                              ;; While we're restoring focus programmatically
                              ;; after applying a suggestion, don't reopen the
                              ;; dropdown — the user just made their choice.
                              (when-not @applying?
                                (reset! open? true)
                                (reset! selected-index 0))
                              (track-cursor! e))
                  :on-click (fn [e]
                              (reset! open? true)
                              (reset! selected-index 0)
                              (track-cursor! e))
                  :on-key-up (fn [e]
                               (track-cursor! e)
                               ;; Cursor-movement keys reopen the dropdown so
                               ;; the user can keep navigating with the keyboard.
                               (when (#{"ArrowLeft" "ArrowRight" "Home" "End"} (.-key e))
                                 (reset! open? true)))
                  :on-select track-cursor!
                  :on-key-down
                  (fn [e]
                    (case (.-key e)
                      "ArrowDown"
                      (when (pos? suggestion-count)
                        (.preventDefault e)
                        (reset! selected-index
                                (mod (inc effective-selected) suggestion-count)))

                      "ArrowUp"
                      (when (pos? suggestion-count)
                        (.preventDefault e)
                        (reset! selected-index
                                (mod (dec effective-selected) suggestion-count)))

                      "Tab"
                      (cond
                        ;; Tab only applies when the dropdown is actually
                        ;; showing — otherwise it should fall through to the
                        ;; default focus-next behavior.
                        (and @open? (pos? suggestion-count))
                        (do (.preventDefault e)
                            (apply-suggestion! (nth suggestions effective-selected)))

                        (and @open? tree-key)
                        (when (apply-tree-top!)
                          (.preventDefault e)))

                      "Enter"
                      (cond
                        ;; Enter while the dropdown is up commits the
                        ;; suggestion; otherwise it fires the search. So
                        ;; Tab-to-complete-then-Enter searches with the
                        ;; completed value, just like clicking the button.
                        (and @open? (pos? suggestion-count))
                        (do (.preventDefault e)
                            (apply-suggestion! (nth suggestions effective-selected)))

                        (and @open? tree-key (apply-tree-top!))
                        (.preventDefault e)

                        :else
                        (do (reset! open? false)
                            (rf/dispatch-sync [::copy-search-string-to-query id])))

                      "Escape"
                      (reset! open? false)

                      nil))
                  :on-change (fn [e]
                               (let [value (-> e .-target .-value)
                                     pos (-> e .-target .-selectionStart)]
                                 (reset! tmp-value value)
                                 (reset! cursor pos)
                                 (reset! selected-index 0)
                                 ;; Typing always reopens the dropdown so that
                                 ;; the close-on-complete behavior from
                                 ;; apply-suggestion! doesn't strand the user.
                                 (reset! open? true)
                                 (rf/dispatch-sync [:set path value])))
                  :style {:outline "none"
                          :border "0"
                          :margin-left "0.5em"
                          :width "calc(100% - 12px - 1.5em)"}}]
         (when (and (= kind :arms) @open? (or tree-key (seq suggestions)))
           [:<>
            ;; (Outside-click is handled at the document level — see
            ;; handle-outside-click above. No overlay div here.)
            (cond
              tree-key
              [:div {:style (merge dropdown-base-style
                                   {:width "25em"
                                    :max-height "25em"
                                    :overflow-y "auto"
                                    :padding "10px"})
                     :on-mouse-down #(.stopPropagation %)}
               [charge-type-select/charge-type-filter-tree
                (fn [name leaf?]
                  (apply-suggestion! (str tree-key ":" (facets/slugify-name name)))
                  (when leaf?
                    (reset! open? false)))
                {:hide-search-bar? true
                 :with-standard-shapes? true}]]

              (seq suggestions)
              [:ul {:style (merge dropdown-base-style
                                  {:right 0
                                   :margin 0
                                   :padding 0
                                   :list-style "none"
                                   :max-height "20em"
                                   :overflow-y "auto"})
                    :on-mouse-down #(.stopPropagation %)}
               (for [[index s] (map-indexed vector suggestions)]
                 ^{:key s}
                 [:li {:style {:padding "0.3em 0.7em"
                               :cursor "pointer"
                               :background (when (= index effective-selected) "#e0e0e0")}
                       :on-click #(apply-suggestion! s)
                       :on-mouse-enter #(reset! selected-index index)}
                  [:code s]])])])]))))

(defn- favorites? [id _options]
  (when @(rf/subscribe [::session/logged-in?])
    (let [on? (get-favorites? id)]
      [:div {:on-click #(rf/dispatch [:set (filter-favorites?-path id) (not on?)])
             :title (tr :string.option/favorites-filter)
             :style {:display "inline-block"
                     :margin-left "10px"
                     :cursor "pointer"}}
       [favorite/icon 20 on?]])))

(defn- ownership [id {:keys [hide-ownership-filter?]
                      :as options}]
  (when (and (not hide-ownership-filter?)
             @(rf/subscribe [::session/logged-in?]))
    [select/raw-select-inline
     {:path (filter-ownership-path id)}
     (get-ownership id options)
     [[:string.option.ownership-filter-choice/all :all]
      [:string.option.ownership-filter-choice/mine :mine]]
     :value-prefix :string.option/show
     :style {:margin-left "10px"
             :margin-bottom "5px"}]))

(defn- access [id {:keys [hide-access-filter?]
                   :as options}]
  (let [consider-filter-access? (and (not hide-access-filter?)
                                     @(rf/subscribe [::session/logged-in?])
                                     (or (= (get-ownership id options) :mine)
                                         @(rf/subscribe [::session/admin?])))]
    (when consider-filter-access?
      [select/raw-select-inline
       {:path (filter-access-path id)}
       (get-access id)
       [[:string.option.access-filter-choice/all :all]
        [:string.option.access-filter-choice/public :public]
        [:string.option.access-filter-choice/private :private]]
       :value-prefix :string.option/access
       :style {:margin-left "10px"
               :margin-bottom "5px"}])))

(defn- sorting [id options]
  [select/raw-select-inline {:path (filter-sorting-path id)}
   (get-sorting id options)
   [[:string.option.sorting-filter-choice/favorites :favorites]
    [:string.option.sorting-filter-choice/creation :created]
    [:string.option.sorting-filter-choice/update :modified]
    [:string.option.sorting-filter-choice/name :name]]
   :value-prefix :string.option/sort-by
   :style {:margin-left "10px"
           :margin-bottom "5px"}])

(defn- close-charge-type-filter! [id pending]
  (when-let [p @pending]
    (rf/dispatch [:set (filter-charge-type-path id) p]))
  (reset! pending nil)
  (rf/dispatch [:set (filter-charge-type-open?-path id) false]))

(defn- charge-type-filter [_id]
  (let [last-select (r/atom nil)
        pending (r/atom nil)]
    (fn [id]
      (let [charge-type (get-charge-type id)
            open? @(rf/subscribe [:get (filter-charge-type-open?-path id)])]
        [:div {:style {:display "inline-block"
                       :margin-left "10px"
                       :position "relative"}}
         [:div {:style {:cursor "pointer"
                        :display "inline-flex"
                        :align-items "center"
                        :gap "4px"}
                :title (tr :string.option/charge-type)
                :on-click (js-event/handled
                           #(do
                              (reset! pending nil)
                              (rf/dispatch [:set (filter-charge-type-open?-path id) (not open?)])))}
          [:i.fas.fa-sitemap {:style {:color (if charge-type
                                               nil
                                               "#999")}}]
          (when charge-type
            [:<>
             [:span charge-type]
             [:span {:on-click (js-event/handled
                                #(rf/dispatch [:set (filter-charge-type-path id) nil]))}
              [:i.fas.fa-times {:style {:font-size "0.8em"}}]]])]
         (when open?
           [:<>
            [:div {:style {:position "fixed"
                           :top 0
                           :left 0
                           :right 0
                           :bottom 0
                           :z-index 99}
                   :on-click #(close-charge-type-filter! id pending)}]
            [:div.charge-type-filter
             {:style {:position "absolute"
                      :top "100%"
                      :left 0
                      :z-index 100
                      :border "1px solid #ddd"
                      :border-radius "5px"
                      :padding "10px"
                      :margin-top "5px"
                      :width "25em"
                      :max-height "25em"
                      :overflow-y "auto"
                      :box-shadow "2px 2px 10px rgba(0,0,0,0.15)"}
              :on-click #(.stopPropagation %)}
             [charge-type-select/charge-type-filter-tree
              (fn [name leaf?]
                (if leaf?
                  (do
                    (reset! pending nil)
                    (rf/dispatch [:set (filter-charge-type-path id) name])
                    (rf/dispatch [:set (filter-charge-type-open?-path id) false]))
                  (let [[prev-name prev-time] @last-select
                        now (.now js/Date)]
                    (reset! pending name)
                    (if (and (= prev-name name)
                             (< (- now prev-time) 500))
                      (do
                        (reset! last-select nil)
                        (rf/dispatch [:set (filter-charge-type-path id) name])
                        (rf/dispatch [:set (filter-charge-type-open?-path id) false]))
                      (reset! last-select [name now])))))]]])]))))

(defn- facet-help-popover []
  (let [open? (r/atom false)]
    (fn []
      [:div {:style {:display "inline-block"
                     :margin-left "10px"
                     :position "relative"}}
       [:i.fas.fa-question-circle
        {:title (tr :string.structured-search/icon-title)
         :style {:cursor "pointer"
                 :color "#888"}
         :on-click (js-event/handled #(swap! open? not))}]
       (when @open?
         [:<>
          [:div {:style {:position "fixed"
                         :top 0 :left 0 :right 0 :bottom 0
                         :z-index 99}
                 :on-click #(reset! open? false)}]
          [:div {:style {:position "absolute"
                         :top "100%"
                         :left 0
                         :z-index 100
                         :background "white"
                         :border "1px solid #ddd"
                         :border-radius "5px"
                         :padding "10px"
                         :margin-top "5px"
                         :width "26em"
                         :box-shadow "2px 2px 10px rgba(0,0,0,0.15)"}
                 :on-click #(.stopPropagation %)}
           [:div {:style {:font-weight "bold" :margin-bottom "6px"}}
            [tr :string.structured-search/title]]
           [:div {:style {:font-size "0.9em" :margin-bottom "8px"}}
            [tr :string.structured-search/description]]
           [:ul {:style {:margin 0 :padding-left "1.2em" :font-size "0.9em"}}
            (for [k facets/facet-keys]
              ^{:key k}
              [:li [:code (str k ":<value>")]])]
           [:div {:style {:font-size "0.85em" :margin-top "8px" :color "#666"}}
            [tr :string.structured-search/example-label] " "
            [:code "smith tincture:or charge:lion"]]]])])))

(defn component [id kind on-select {:keys [component-styles]
                                    :as options}]
  [:div.filter-component {:style component-styles}
   [:div.filter-component-search
    [search-input id kind options]

    (when (= kind :charge)
      [charge-type-filter id])

    (when (= kind :arms)
      [facet-help-popover])

    [:button.button.primary
     {:on-click #(rf/dispatch [::copy-search-string-to-query id])
      :style {:margin-left "10px"}}
     "search"]

    [list-mode id options]

    [favorites? id options]

    [ownership id options]

    [access id options]

    [sorting id options]

    [results-count id kind options]]

   [results id kind on-select options]])

(rf/reg-event-db ::restore-from-url-parameters
  (fn [db [_ id]]
    (let [qs (subs (.-search js/window.location) 1)
          data (parameters/query-string->map qs)]
      (reduce (fn [db [k v]]
                (case k
                  :access (assoc-in db (filter-access-path id) v)
                  :favorites (assoc-in db (filter-favorites?-path id) v)
                  :mode (assoc-in db (filter-list-mode-path id) v)
                  :ownership (assoc-in db (filter-ownership-path id) v)
                  :sort (assoc-in db (filter-sorting-path id) v)
                  :tags (assoc-in db (filter-tags-path id) v)
                  :charge-type (assoc-in db (filter-charge-type-path id) v)
                  :q (-> db
                         (assoc-in (filter-temporary-search-string-path id) v)
                         (assoc-in (filter-search-string-path id) v))
                  db))
              (update-in db [:ui :filter] dissoc id)
              data))))
