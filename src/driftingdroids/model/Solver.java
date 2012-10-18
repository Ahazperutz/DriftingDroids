/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011, 2012  Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package driftingdroids.model;

import java.util.Collections;
import java.util.Formatter;
import java.util.List;



public abstract class Solver {
    
    public enum SOLUTION_MODE {
        MINIMUM("minimum", "solver.Minimum.text"), MAXIMUM("maximum", "solver.Maximum.text");
        private final String name, l10nKey;
        private SOLUTION_MODE(String name, String l10nKey) { this.name = name;  this.l10nKey = l10nKey; }
        @Override public String toString() { return Board.L10N.getString(this.l10nKey); }
        public String getName() { return this.name; }
    }
    
    
    protected final Board board;
    protected final byte[][] boardWalls;
    protected final int boardSizeNumBits;
    protected final int boardSizeBitMask;
    protected final int boardNumRobots;
    protected final boolean isBoardStateInt32;
    protected final boolean isBoardGoalWildcard;
    
    protected SOLUTION_MODE optSolutionMode = SOLUTION_MODE.MINIMUM;
    protected boolean optAllowRebounds = true;
    
    protected List<Solution> lastResultSolutions = null;
    protected long solutionMilliSeconds = 0;
    protected int solutionStoredStates = 0;
    
    
    
    public static Solver createInstance(final Board board) {
        return (board.isSolution01() ? new SolverIDDFS(board) : new SolverBFS(board));
    }
    
    
    
    public abstract List<Solution> execute() throws InterruptedException;
    
    
    
    protected Solver(final Board board) {
        this.board = board;
        this.boardWalls = this.board.getWalls();
        this.boardSizeNumBits = 32 - Integer.numberOfLeadingZeros(this.board.size - 1); //ceil(log2(x))
        int bitMask = 0;
        for (int i = 0;  i < this.boardSizeNumBits;  ++i) { bitMask += bitMask + 1; }
        this.boardSizeBitMask = bitMask;
        this.boardNumRobots = this.board.getRobotPositions().length;
        this.isBoardStateInt32 = (this.boardSizeNumBits * this.boardNumRobots <= 32);
        this.isBoardGoalWildcard = (this.board.getGoal().robotNumber < 0);
    }

    protected final String stateString(final int[] state) {
        final Formatter formatter = new Formatter();
        this.swapGoalLast(state);
        for (int i = 0;  i < state.length;  i++) {
            formatter.format("%02x", Integer.valueOf(state[i]));
        }
        this.swapGoalLast(state);
        return "0x" + formatter.out().toString();
    }
    
    protected final void swapGoalLast(final int[] state) {
        //swap goal robot and last robot (if goal is not wildcard)
        if (false == this.isBoardGoalWildcard) {
            final int tmp = state[state.length - 1];
            state[state.length - 1] = state[this.board.getGoal().robotNumber];
            state[this.board.getGoal().robotNumber] = tmp;
        }
    }
    
    protected final void sortSolutions() {
        if (0 == this.lastResultSolutions.size()) {
            this.lastResultSolutions.add(new Solution(this.board));
        }
        if (SOLUTION_MODE.MINIMUM == this.optSolutionMode) {
            Collections.sort(this.lastResultSolutions);
        } else if (SOLUTION_MODE.MAXIMUM == this.optSolutionMode) {
            Collections.sort(this.lastResultSolutions, Collections.reverseOrder());
        }
    }
    
    
    
    public final List<Solution> get() {
        return this.lastResultSolutions;
    }
    
    public final void setOptionSolutionMode(SOLUTION_MODE mode) {
        this.optSolutionMode = mode;
    }
    
    public final SOLUTION_MODE getOptionSolutionMode() {
        return this.optSolutionMode;
    }
    
    public final void setOptionAllowRebounds(boolean allowRebounds) {
        this.optAllowRebounds = allowRebounds;
    }
    
    public final boolean getOptionAllowRebounds() {
        return this.optAllowRebounds;
    }
    
    public final String getOptionsAsString() {
        return this.optSolutionMode.getName() + " number of robots moved; "
                + (this.optAllowRebounds ? "with" : "no") + " rebound moves";
    }
    
    public final long getSolutionMilliSeconds() {
        return this.solutionMilliSeconds;
    }
    
    public final int getSolutionStoredStates() {
        return this.solutionStoredStates;
    }
    
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("storedStates=").append(this.solutionStoredStates);
        s.append(", time=").append(this.solutionMilliSeconds / 1000d).append(" seconds");
        return s.toString();
    }
}
