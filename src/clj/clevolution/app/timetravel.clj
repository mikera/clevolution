(ns clevolution.app.timetravel
  (:require [clevolution.app.appstate :refer [app-state]]))


(def app-history (atom [@app-state]))
(def app-future (atom []))

(def ignore (atom {:time-machine false}))


(defn forget-everything! []
  (reset! app-future [])
  (reset! app-history []))


(defn undo-is-possible
  []
  (> (count @app-history) 1))

(defn redo-is-possible
  []
  (> (count @app-future) 0))


(defn push-onto-undo-stack
  [new-state]
  (let [old-watchable-app-state (last @app-history)]
    (when-not (= old-watchable-app-state new-state)
      (swap! app-history conj new-state))))

(defn replace-last-state
  [new-state]
  (reset! app-history (conj (vec (butlast @app-history))
                            new-state)))

;; undo and redo cause state changes that we want our watch-fn to ignore,
;; hence the ignore atom

(defn do-undo
  []
  (if (undo-is-possible)
    (do
      (println "UNDO")
      (swap! app-future conj (last @app-history))
      (swap! app-history pop)
      (swap! ignore assoc :time-machine true)
      (reset! app-state (last @app-history)))
    (println "can't undo: at initial state")))

(defn do-redo
  []
  (if (redo-is-possible)
    (do
      (println "REDO")
      (swap! ignore assoc :time-machine true)
      (reset! app-state (last @app-future))
      (push-onto-undo-stack (last @app-future))
      (swap! app-future pop))
    (println "can't redo: at newest state")))

(defn do-rewind
  []
  (while (do-undo)))


(def watch-fn (fn [_ _ old-state new-state]
                (if (not (@ignore :time-machine))
                  (cond
                    ;; there is new state data:
                    (or (not= (:generator old-state) (:generator new-state))
                        (not= (:viewport old-state) (:viewport new-state))
                        (not= (:panel old-state) (:panel new-state)))
                    (do
                      (println "NEW STATE")
                      (reset! app-future [])
                      (push-onto-undo-stack new-state))

                    ;; image has been updated in response to new state data:
                    (not= (:image old-state) (:image new-state))
                    (do
                      (println "Only image changed: amending state")
                      (replace-last-state new-state))

                    :else
                    (println "no change"))

                  (println "no new state"))
                (swap! ignore assoc :time-machine false)))


(add-watch app-state :time-machine watch-fn)
